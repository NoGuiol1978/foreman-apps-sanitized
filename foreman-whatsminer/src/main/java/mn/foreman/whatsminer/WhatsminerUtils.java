package mn.foreman.whatsminer;

import mn.foreman.cgminer.Context;
import mn.foreman.cgminer.ContextKey;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.miners.FanInfo;
import mn.foreman.model.miners.asic.Asic;
import mn.foreman.util.RateUnit;
import mn.foreman.whatsminer.latest.Command;
import mn.foreman.whatsminer.latest.WhatsminerApi;

import org.apache.http.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/** Utility methods for parsing Whatsminer miner response values. */
class WhatsminerUtils {

    /**
     * Obtains error codes for newer gen miners.
     *
     * @param ip            The ip.
     * @param port          The port.
     * @param password      The password.
     * @param configuration The configuration.
     *
     * @return The error codes.
     */
    @SuppressWarnings("unchecked")
    static List<String> getErrorCodes(
            final String ip,
            final int port,
            final String password,
            final ApplicationConfiguration configuration) {
        final List<String> errorCodes = new LinkedList<>();
        try {
            WhatsminerApi.runCommand(
                    ip,
                    port,
                    password,
                    Command.GET_ERROR_CODE,
                    Collections.emptyMap(),
                    configuration,
                    response -> {
                        final List<Map<String, String>> codes =
                                (List<Map<String, String>>) ((Map<String, Object>) response).get("error_code");
                        errorCodes.addAll(
                                codes
                                        .stream()
                                        .map(Map::keySet)
                                        .flatMap(Set::stream)
                                        .collect(Collectors.toList()));
                    });
        } catch (final Exception e) {
            // Ignore
        }
        return errorCodes;
    }

    /**
     * Obtains the PSU fan.
     *
     * @param ip            The ip.
     * @param port          The port.
     * @param password      The password.
     * @param configuration The configuration.
     * @param fanSpeeds     The fan speeds.
     * @param builder       The builder.
     */
    @SuppressWarnings("unchecked")
    static void getPsuFan(
            final String ip,
            final int port,
            final String password,
            final ApplicationConfiguration configuration,
            final List<String> fanSpeeds,
            final Asic.Builder builder) {
        try {
            WhatsminerApi.runCommand(
                    ip,
                    port,
                    password,
                    Command.GET_PSU,
                    Collections.emptyMap(),
                    configuration,
                    response -> {
                        final Map<String, Object> map =
                                (Map<String, Object>) response;
                        if (map.containsKey("serial_no")) {
                            builder.setPsuSerial(map.get("serial_no").toString());
                        }

                        if (map.containsKey("fan_speed")) {
                            // Only store if non-zero
                            final String candidate = map.get("fan_speed").toString();
                            if (candidate != null && !candidate.isEmpty() && !"0".equals(candidate)) {
                                fanSpeeds.add(candidate);
                            }
                        } else {
                            // Bug in the 20220104 firmware, so substitute a
                            // 0 fan
                            fanSpeeds.add("0");
                        }
                    });
        } catch (final Exception e) {
            // Ignore
        }
    }

    /**
     * Obtains the firmware version.
     *
     * @param ip            The ip.
     * @param port          The port.
     * @param password      The password.
     * @param configuration The configuration.
     * @param builder       The builder.
     */
    @SuppressWarnings("unchecked")
    static void getStatus(
            final String ip,
            final int port,
            final String password,
            final ApplicationConfiguration configuration,
            final Asic.Builder builder) {
        try {
            WhatsminerApi.runCommand(
                    ip,
                    port,
                    password,
                    Command.STATUS,
                    Collections.emptyMap(),
                    configuration,
                    response -> {
                        final Object version = ((Map<String, Object>) response).get("Firmware Version");
                        if (version != null) {
                            builder.setCompileTime(version.toString().replace("'", ""));
                        }
                    });
        } catch (final Exception e) {
            // Ignore
        }
    }

    /**
     * Creates a token query.
     *
     * @param uri   The uri.
     * @param token The token to set.
     *
     * @return The query.
     */
    static WhatsminerQuery.Query queryToken(
            final String uri,
            final AtomicReference<String> token) {
        return WhatsminerQuery.Query
                .builder()
                .uri(uri)
                .isGet(true)
                .isMultipartForm(false)
                .urlParams(Collections.emptyList())
                .callback((code, data) -> {
                    if (code == HttpStatus.SC_OK) {
                        final Document document =
                                Jsoup.parse(data);
                        final Element form =
                                document.select("form").first();
                        if (form != null) {
                            final Element input =
                                    document.select("input[name=token]").first();
                            token.set(input.attr("value"));
                        } else {
                            // No form - attempt to extract from js
                            final int start = data.indexOf("token");
                            final int tokenStart = data.indexOf("'", start);
                            final int tokenEnd = data.indexOf("'", tokenStart + 1);
                            token.set(
                                    data.substring(
                                            tokenStart,
                                            tokenEnd).replace("'", ""));
                        }
                    }
                })
                .build();
    }

    /**
     * Updates the builder with dev details.
     *
     * @param values The response values.
     */
    static void updateDevDetails(
            final Map<String, List<Map<String, String>>> values,
            final Asic.Builder builder) {
        values.entrySet()
                .stream()
                .filter(entry -> "DEVDETAILS".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .findFirst()
                .ifPresent(stringStringMap ->
                        builder.setMinerType(
                                stringStringMap.getOrDefault(
                                        "Model",
                                        "")));
    }

    /**
     * Updates the builder with devs.
     *
     * @param values   The response values.
     * @param builder  The builder for metrics.
     * @param failover Whether this is being used for failover metrics.
     */
    static void updateDevs(
            final Map<String, List<Map<String, String>>> values,
            final Asic.Builder builder,
            final boolean failover) {
        final AtomicReference<BigDecimal> hashRate =
                new AtomicReference<>(BigDecimal.ZERO);
        final AtomicInteger fanIn = new AtomicInteger(0);
        final AtomicInteger fanOut = new AtomicInteger(0);
        final AtomicInteger activeBoards = new AtomicInteger(0);
        values.entrySet()
                .stream()
                .filter(entry -> "DEVS".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .forEach(map -> {
                    builder.addTemp(map.get("Temperature"), true);
                    builder.addTemp(map.get("Chip Temp Avg"), true);

                    fanIn.set(
                            Integer.parseInt(
                                    map.getOrDefault(
                                            "Fan Speed In",
                                            "0")));
                    fanOut.set(
                            Integer.parseInt(
                                    map.getOrDefault(
                                            "Fan Speed Out",
                                            "0")));

                    final BigDecimal boardHashRate =
                            new BigDecimal(
                                    map.getOrDefault(
                                            "MHS av",
                                            "0"))
                                    .multiply(BigDecimal.valueOf(1000 * 1000));
                    if (boardHashRate.compareTo(BigDecimal.ZERO) > 0) {
                        activeBoards.incrementAndGet();
                    }

                    hashRate.set(hashRate.get().add(boardHashRate));

                    if (!"Alive".equals(map.getOrDefault("Status", "Alive"))) {
                        builder.hasErrors(true);
                    }
                });

        if (failover) {
            builder
                    .setHashRate(hashRate.get())
                    .setFanInfo(
                            new FanInfo.Builder()
                                    .setCount(3)
                                    .addSpeed(fanIn.get())
                                    .addSpeed(fanOut.get())
                                    .addSpeed(0, true)
                                    .setSpeedUnits("RPM")
                                    .build());
        }
        builder.setBoards(activeBoards.get());
    }

    /**
     * Updates the builder with stats.
     *
     * @param values The response values.
     */
    static void updateStats(
            final Map<String, List<Map<String, String>>> values,
            final Asic.Builder builder) {
        final AtomicInteger activeBoards = new AtomicInteger(0);
        values.entrySet()
                .stream()
                .filter(entry -> "STATS".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .filter(map -> map.containsKey("STATS"))
                .forEach(map -> {
                    if (map.containsKey("slot")) {
                        activeBoards.incrementAndGet();
                    }

                    for (int i = 1; i <= 8; i++) {
                        builder.addTemp(map.get("temp_" + i));
                    }
                    if (!"0".equals(map.getOrDefault("err_chips", "0"))) {
                        builder.hasErrors(true);
                    }
                });

        builder.setBoards(activeBoards.get());
    }

    /**
     * Updates the builder with summary info.
     *
     * @param ip            The IP.
     * @param port          The port.
     * @param password      The password.
     * @param configuration The configuration.
     * @param values        The response values.
     * @param builder       The builder.
     * @param context       The context.
     */
    static void updateSummary(
            final String ip,
            final int port,
            final String password,
            final ApplicationConfiguration configuration,
            final Map<String, List<Map<String, String>>> values,
            final Asic.Builder builder,
            final Context context) {
        values.entrySet()
                .stream()
                .filter(entry -> "SUMMARY".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .filter(map -> map.containsKey("MHS av"))
                .forEach(map -> {
                    context.addSimple(ContextKey.MAC, map.get("MAC"));

                    if (map.containsKey("Firmware Version")) {
                        // 202008
                        builder.setCompileTime(map.get("Firmware Version").replace("'", ""));
                    } else {
                        // 202201
                        getStatus(
                                ip,
                                port,
                                password,
                                configuration,
                                builder);
                    }

                    final BigDecimal mhsAv =
                            new BigDecimal(map.get("MHS av"))
                                    .multiply(BigDecimal.valueOf(Math.pow(1000, 2)));
                    final String fanSpeedIn =
                            map.getOrDefault("Fan Speed In", "");
                    final String fanSpeedOut =
                            map.getOrDefault("Fan Speed Out", "");

                    final List<String> fans = new LinkedList<>();
                    addFan(fanSpeedIn, fans);
                    addFan(fanSpeedOut, fans);
                    if (map.containsKey("Power Fanspeed")) {
                        // 202008
                        addFan(
                                map.getOrDefault(
                                        "Power Fanspeed",
                                        ""), fans);
                        final int errorCodes =
                                Integer.parseInt(
                                        map.getOrDefault("Error Code Count", "0"));
                        for (int i = 0; i < errorCodes; i++) {
                            builder.addErrorCode(
                                    map.get(
                                            String.format(
                                                    "Error Code %d",
                                                    i)));
                        }
                    } else {
                        // 202201
                        getPsuFan(
                                ip,
                                port,
                                password,
                                configuration,
                                fans,
                                builder);
                        getErrorCodes(
                                ip,
                                port,
                                password,
                                configuration)
                                .forEach(builder::addErrorCode);
                    }

                    final String factoryGhs = map.get("Factory GHS");
                    if (factoryGhs != null && !factoryGhs.isEmpty()) {
                        builder.setHashRateIdeal(
                                RateUnit.GHS.getMultiplier() *
                                        Double.parseDouble(factoryGhs));
                    }

                    final boolean inferredLiquid =
                            "0".equals(fanSpeedIn) &&
                                    "0".equals(fanSpeedOut) &&
                                    mhsAv.compareTo(BigDecimal.ZERO) > 0;

                    builder
                            .setPowerMode(toPowerMode(map.get("Power Mode")))
                            .setFanInfo(
                                    new FanInfo.Builder()
                                            .setCount(fans.size())
                                            .addSpeeds(fans, true)
                                            .setSpeedUnits("RPM")
                                            .build())
                            .addTemp(map.get("Env Temp"))
                            .setHashRate(mhsAv)
                            .setPower(map.get("Power"))
                            .setPowerLimit(map.get("Power Limit"))
                            .setPowerRt(map.get("Power_RT"))
                            .setVoltage(map.get("Voltage"))
                            .setLiquidCooling(
                                    map.getOrDefault(
                                            "Liquid Cooling",
                                            Boolean.toString(inferredLiquid)));
                });
    }

    /**
     * Adds the provided fans.
     *
     * @param fan  The value.
     * @param fans The dest.
     */
    private static void addFan(
            final String fan,
            final List<String> fans) {
        if (fan != null && !fan.isEmpty() && !"0".equals(fan)) {
            fans.add(fan);
        }
    }

    /**
     * Converts the provided mode to a string.
     *
     * @param powerMode The mode.
     *
     * @return The power mode.
     */
    private static Asic.PowerMode toPowerMode(final String powerMode) {
        if ("High".equalsIgnoreCase(powerMode)) {
            return Asic.PowerMode.HIGH;
        } else if ("Low".equalsIgnoreCase(powerMode)) {
            return Asic.PowerMode.LOW;
        } else {
            return Asic.PowerMode.NORMAL;
        }
    }
}