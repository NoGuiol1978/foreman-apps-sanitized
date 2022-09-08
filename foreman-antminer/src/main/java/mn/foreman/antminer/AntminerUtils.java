package mn.foreman.antminer;

import mn.foreman.antminer.braiins.BraiinsType;
import mn.foreman.antminer.vnish.v3.VnishV3Api;
import mn.foreman.cgminer.CgMiner;
import mn.foreman.cgminer.Context;
import mn.foreman.cgminer.request.CgMinerCommand;
import mn.foreman.cgminer.request.CgMinerRequest;
import mn.foreman.io.Query;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.error.EmptySiteException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/** A collection of utility functions for antminer processing. */
public class AntminerUtils {

    /** The regex pattern to detecting asicseer. */
    private static final Pattern ASIC_SEER_PATTERN =
            Pattern.compile("\\d+\\.\\d+\\.\\d+-\\w+-\\d+\\.\\d+\\.?\\d?-\\w+");

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(AntminerUtils.class);

    /** The mapper for parsing json. */
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    /** The type key. */
    private static final String TYPE = "Type";

    /**
     * Gets the bOS type.
     *
     * @param versionType  The version type.
     * @param ip           The ip.
     * @param port         The port.
     * @param typeCallback The callback.
     * @param timeConfig   The socket config.
     *
     * @return The type.
     */
    public static Optional<AntminerType> getBraiinsType(
            final AtomicReference<String> versionType,
            final String ip,
            final int port,
            final TypeCallback typeCallback,
            final ApplicationConfiguration.TimeConfig timeConfig) {
        final AtomicReference<AntminerType> type = new AtomicReference<>();
        final String cgType = versionType.get();
        if (cgType != null) {
            // Older version
            BraiinsType
                    .toType(cgType)
                    .map(BraiinsType::getType)
                    .ifPresent(antminerType -> {
                        type.set(antminerType);
                        typeCallback.accept(
                                versionType.get(),
                                "",
                                antminerType.getIdentifier());
                    });
        } else {
            // Newer, so query 'devs'
            final CgMiner cgMiner =
                    new CgMiner.Builder(new Context(), Collections.emptyList())
                            .setApiIp(ip)
                            .setApiPort(port)
                            .setConnectTimeout(timeConfig)
                            .addRequest(
                                    new CgMinerRequest.Builder()
                                            .setCommand(CgMinerCommand.DEVS)
                                            .build(),
                                    (builder, response) ->
                                            response
                                                    .getStatus()
                                                    .stream()
                                                    .filter(map -> map.containsKey("Description"))
                                                    .map(map -> map.get("Description"))
                                                    .findFirst()
                                                    .ifPresent(s -> {
                                                        final Optional<AntminerType> antminerType =
                                                                BraiinsType.toType(s)
                                                                        .map(BraiinsType::getType);
                                                        antminerType.ifPresent(
                                                                value -> {
                                                                    type.set(value);
                                                                    typeCallback.accept(
                                                                            s,
                                                                            "",
                                                                            value.getIdentifier());
                                                                });
                                                    }))
                            .addRequest(
                                    new CgMinerRequest.Builder()
                                            .setCommand(CgMinerCommand.DEVDETAILS)
                                            .build(),
                                    (builder, response) ->
                                            response
                                                    .getValues()
                                                    .entrySet()
                                                    .stream()
                                                    .filter(entry -> entry.getKey().equals("DEVDETAILS"))
                                                    .map(Map.Entry::getValue)
                                                    .flatMap(List::stream)
                                                    .filter(map -> map.containsKey("Model"))
                                                    .map(map -> map.get("Model"))
                                                    .findFirst()
                                                    .ifPresent(s -> {
                                                        final Optional<AntminerType> antminerType =
                                                                BraiinsType.toType(s)
                                                                        .map(BraiinsType::getType);
                                                        antminerType.ifPresent(
                                                                value -> {
                                                                    type.set(value);
                                                                    typeCallback.accept(
                                                                            s + " (braiins)",
                                                                            "",
                                                                            value.getIdentifier());
                                                                });
                                                    }))
                            .build();

            try {
                cgMiner.getStats();
            } catch (final Exception e) {
                // Ignore
            }
        }
        return Optional.ofNullable(type.get());
    }

    /**
     * Obtains the miner conf.
     *
     * @param ip         The IP.
     * @param port       The port.
     * @param realm      The realm.
     * @param uri        The URI.
     * @param username   The username.
     * @param password   The password.
     * @param timeConfig The socket config.
     *
     * @return The conf.
     *
     * @throws Exception on failure.
     */
    public static Map<String, Object> getConf(
            final String ip,
            final int port,
            final String realm,
            final String uri,
            final String username,
            final String password,
            final ApplicationConfiguration.TimeConfig timeConfig) throws Exception {
        final AtomicReference<Map<String, Object>> minerConfRef =
                new AtomicReference<>();
        Query.digestGet(
                ip,
                port,
                realm,
                uri,
                username,
                password,
                (code, s) -> {
                    try {
                        minerConfRef.set(
                                OBJECT_MAPPER.readValue(
                                        // Patch fan-ctrl, if needed
                                        s.replace(": ,", ": false,"),
                                        new TypeReference<Map<String, Object>>() {

                                        }));
                    } catch (final IOException e) {
                        LOG.warn("Exception occurred while querying", e);
                    }
                },
                timeConfig.getTimeout(),
                timeConfig.getTimeoutUnits());
        return minerConfRef.get();
    }

    /**
     * Obtains the raw response.
     *
     * @param ip         The IP.
     * @param port       The port.
     * @param realm      The realm.
     * @param uri        The URI.
     * @param username   The username.
     * @param password   The password.
     * @param timeConfig The socket config.
     *
     * @return The raw response.
     *
     * @throws Exception on failure.
     */
    public static Optional<String> getRaw(
            final String ip,
            final int port,
            final String realm,
            final String uri,
            final String username,
            final String password,
            final ApplicationConfiguration.TimeConfig timeConfig) throws Exception {
        final AtomicReference<String> raw = new AtomicReference<>();
        Query.digestGet(
                ip,
                port,
                realm,
                uri,
                username,
                password,
                (code, s) -> {
                    if (code == HttpStatus.SC_OK) {
                        raw.set(s);
                    }
                },
                timeConfig.getTimeout(),
                timeConfig.getTimeoutUnits());
        return Optional.ofNullable(raw.get());
    }

    /**
     * Gets the type.
     *
     * @param ip            The ip.
     * @param port          The port.
     * @param webPort       The web port.
     * @param realm         The realm.
     * @param username      The username.
     * @param password      The password.
     * @param typeCallback  The callback.
     * @param configuration The configuration.
     *
     * @return The type.
     */
    public static Optional<AntminerType> getStockType(
            final String ip,
            final int port,
            final int webPort,
            final String realm,
            final String username,
            final String password,
            final TypeCallback typeCallback,
            final ApplicationConfiguration configuration) {
        final AtomicReference<AntminerType> type = new AtomicReference<>();

        final AtomicReference<String> hashRateIdeal
                = new AtomicReference<>();
        final Map<String, List<Map<String, String>>> versionData =
                new HashMap<>();

        final CgMiner cgMiner =
                new CgMiner.Builder(new Context(), Collections.emptyList())
                        .setApiIp(ip)
                        .setApiPort(port)
                        .setConnectTimeout(configuration.getReadSocketTimeout())
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.VERSION)
                                        .build(),
                                (builder, response) -> versionData.putAll(response.getValues()))
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.STATS)
                                        .build(),
                                (builder, response) -> {
                                    // Get the ideal hash rate
                                    response
                                            .getValues()
                                            .entrySet()
                                            .stream()
                                            .filter(entry -> entry.getKey().equals("STATS"))
                                            .map(Map.Entry::getValue)
                                            .flatMap(List::stream)
                                            .filter(map -> map.containsKey("total_rateideal"))
                                            .map(map -> map.get("total_rateideal"))
                                            .findFirst()
                                            .ifPresent(hashRateIdeal::set);

                                    // Could also be vnish
                                    response
                                            .getValues()
                                            .entrySet()
                                            .stream()
                                            .filter(entry -> entry.getKey().equals("STATS"))
                                            .map(Map.Entry::getValue)
                                            .flatMap(List::stream)
                                            .filter(map -> map.containsKey("Type"))
                                            .map(map -> map.get("Type"))
                                            .filter(candidate -> candidate.toLowerCase().contains("vnish"))
                                            .findFirst()
                                            .ifPresent(s -> {
                                                versionData.clear();
                                                versionData.putAll(response.getValues());
                                            });
                                })
                        .build();

        try {
            cgMiner.getStats();
        } catch (final Exception e) {
            // Possibly S19 vnish
            try {
                final Optional<VnishV3Api.Context> contextOptional =
                        VnishV3Api.login(
                                ip,
                                webPort,
                                password,
                                configuration);
                if (contextOptional.isPresent()) {
                    final Optional<VnishV3Api.Summary> summaryOptional =
                            VnishV3Api.summary(contextOptional.get());
                    if (summaryOptional.isPresent()) {
                        final VnishV3Api.Summary summary = summaryOptional.get();
                        if (summary.miner != null) {
                            typeCallback.accept(
                                    summary.miner.minerType,
                                    summary.miner.compileTime,
                                    summary.miner.minerType);
                            AntminerType
                                    .forModel("", summary.miner.minerType)
                                    .ifPresent(type::set);
                        } else {
                            // No fans, not initialized, so default
                            typeCallback.accept(
                                    AntminerType.ANTMINER_S19J_PRO_VNISH.getIdentifier(),
                                    "",
                                    AntminerType.ANTMINER_S19J_PRO_VNISH.getIdentifier());
                            type.set(AntminerType.ANTMINER_S19J_PRO_VNISH);
                        }
                    }
                }
            } catch (final Exception e2) {
                // Ignore
            }
        }

        if (type.get() == null) {
            try {
                AntminerUtils.toType(
                        versionData,
                        hashRateIdeal,
                        typeCallback).ifPresent(type::set);
            } catch (final EmptySiteException e) {
                // Ignore
            }

            if (type.get() == null) {
                // Attempt to use the system info
                try {
                    getSystemAttribute(
                            ip,
                            webPort,
                            username,
                            password,
                            realm,
                            "minertype",
                            configuration.getReadSocketTimeout()).ifPresent(
                            value -> {
                                typeCallback.accept(
                                        "",
                                        "",
                                        value);
                                AntminerType.forModel("Type", value).ifPresent(type::set);
                            });
                } catch (final Exception e) {
                    LOG.warn("Exception occurred while querying", e);
                }
            }
        }

        return Optional.ofNullable(type.get());
    }

    /**
     * Returns a system attribute.
     *
     * @param ip         The IP.
     * @param port       The port.
     * @param username   The username.
     * @param password   The password.
     * @param realm      The realm.
     * @param key        The key.
     * @param timeConfig The socket config.
     *
     * @return The attribute value.
     */
    public static Optional<String> getSystemAttribute(
            final String ip,
            final int port,
            final String username,
            final String password,
            final String realm,
            final String key,
            final ApplicationConfiguration.TimeConfig timeConfig) {
        final AtomicReference<String> attribute = new AtomicReference<>();
        try {
            Query.digestGet(
                    ip,
                    port,
                    realm,
                    "/cgi-bin/get_system_info.cgi",
                    username,
                    password,
                    (code, s) -> {
                        try {
                            final Map<String, Object> conf =
                                    OBJECT_MAPPER.readValue(
                                            s,
                                            new TypeReference<Map<String, Object>>() {
                                            });
                            final String typeValue =
                                    conf.getOrDefault(
                                            key,
                                            "").toString();
                            attribute.set(typeValue);
                        } catch (final Exception e) {
                            // Ignore if we can't get
                        }
                    },
                    timeConfig.getTimeout(),
                    timeConfig.getTimeoutUnits());
        } catch (final Exception e) {
            // Ignore if we can't get
        }
        return Optional.ofNullable(attribute.get());
    }

    /**
     * Gets the type.
     *
     * @param ip            The ip.
     * @param port          The port.
     * @param webPort       The web port.
     * @param realm         The realm.
     * @param username      The username.
     * @param password      The password.
     * @param typeCallback  The callback.
     * @param configuration The socket config.
     *
     * @return The type.
     */
    public static Optional<AntminerType> getType(
            final String ip,
            final int port,
            final int webPort,
            final String realm,
            final String username,
            final String password,
            final TypeCallback typeCallback,
            final ApplicationConfiguration configuration) {
        // First, determine what firmware
        final AtomicReference<AntminerType> typeAtomicReference =
                new AtomicReference<>();
        final AtomicBoolean isBraiins = new AtomicBoolean(false);
        final AtomicReference<String> braiinsType = new AtomicReference<>();
        final CgMiner cgMiner =
                new CgMiner.Builder(new Context(), Collections.emptyList())
                        .setApiIp(ip)
                        .setApiPort(port)
                        .setConnectTimeout(configuration.getReadSocketTimeout())
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.VERSION)
                                        .build(),
                                (builder, response) -> {
                                    isBraiins.set(
                                            isBraiins(
                                                    response.getValues(),
                                                    braiinsType));
                                    if (!isBraiins.get()) {
                                        // Might be seer
                                        response
                                                .getValues()
                                                .entrySet()
                                                .stream()
                                                .filter(entry -> "VERSION".equals(entry.getKey()))
                                                .map(Map.Entry::getValue)
                                                .flatMap(List::stream)
                                                .filter(map -> map.containsKey("BMMiner"))
                                                .filter(map -> ASIC_SEER_PATTERN.matcher(map.get("BMMiner")).find())
                                                .findFirst()
                                                .ifPresent(map -> {
                                                    final AntminerType newType =
                                                            AntminerType.forSeerModel(map.get("Type")).orElse(null);
                                                    if (newType != null) {
                                                        typeAtomicReference.set(newType);
                                                        typeCallback.accept(
                                                                map.get("Type"),
                                                                map.get("BMMiner"),
                                                                map.get("Type"));
                                                    }
                                                });
                                    }
                                })
                        .build();

        try {
            cgMiner.getStats();
        } catch (final Exception e) {
            LOG.warn("Exception occurred while querying", e);
        }

        Optional<AntminerType> type =
                Optional.ofNullable(typeAtomicReference.get());
        if (!type.isPresent()) {
            if (isBraiins.get()) {
                type =
                        getBraiinsType(
                                braiinsType,
                                ip,
                                port,
                                typeCallback,
                                configuration.getReadSocketTimeout());
            } else {
                type =
                        getStockType(
                                ip,
                                port,
                                webPort,
                                realm,
                                username,
                                password,
                                typeCallback,
                                configuration);
            }
        }

        return type;
    }

    /**
     * Obtains the new generation miner types.
     *
     * @param ip         The ip.
     * @param port       The port.
     * @param realm      The realm.
     * @param username   The username.
     * @param password   The password.
     * @param timeConfig The time config.
     *
     * @return The types.
     */
    public static Map<String, String> getTypes(
            final String ip,
            final int port,
            final String realm,
            final String username,
            final String password,
            final ApplicationConfiguration.TimeConfig timeConfig) {
        final AtomicReference<Map<String, String>> minerConfRef =
                new AtomicReference<>(new HashMap<>());
        try {
            Query.digestGet(
                    ip,
                    port,
                    realm,
                    "/cgi-bin/miner_type.cgi",
                    username,
                    password,
                    (code, s) -> {
                        try {
                            minerConfRef.set(
                                    OBJECT_MAPPER.readValue(
                                            s,
                                            new TypeReference<Map<String, String>>() {
                                            }));
                        } catch (final IOException e) {
                            LOG.warn("Exception occurred while querying", e);
                        }
                    },
                    timeConfig.getTimeout(),
                    timeConfig.getTimeoutUnits());
        } catch (final Exception e) {
            // Ignore
        }
        return minerConfRef.get();
    }

    /**
     * Checks to see if the miner is a new generation.
     *
     * @param conf       The conf.
     * @param parameters The command arguments.
     *
     * @return Whether or not the miner is a new gen.
     */
    public static boolean isNewGen(
            final Map<String, Object> conf,
            final Map<String, Object> parameters) {
        final String slug = parameters.getOrDefault("slug", "").toString();
        return conf.containsKey("bitmain-pwth") ||
                slug.contains("s19") ||
                slug.contains("d7") ||
                slug.contains("l7");
    }

    /**
     * Checks to see if the miner is a new generation.
     *
     * @param ip         The ip.
     * @param port       The port.
     * @param realm      The realm.
     * @param username   The username.
     * @param password   The password.
     * @param timeConfig The socket config.
     * @param parameters The command arguments.
     *
     * @return Whether or not the miner is a new gen.
     *
     * @throws Exception on failure.
     */
    public static boolean isNewGen(
            final String ip,
            final int port,
            final String realm,
            final String username,
            final String password,
            final ApplicationConfiguration.TimeConfig timeConfig,
            final Map<String, Object> parameters)
            throws Exception {
        return isNewGen(
                getConf(
                        ip,
                        port,
                        realm,
                        "/cgi-bin/get_miner_conf.cgi",
                        username,
                        password,
                        timeConfig),
                parameters);
    }

    /**
     * Gets the version info from the response values.
     *
     * @param responseValues The response values.
     *
     * @return The version info, if found.
     *
     * @throws EmptySiteException if not found.
     */
    static Map<String, String> toVersion(
            final Map<String, List<Map<String, String>>> responseValues)
            throws EmptySiteException {
        return responseValues
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().equals("VERSION") || entry.getKey().equals("STATS"))
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .filter(AntminerUtils::isKnown)
                .findFirst()
                .orElseThrow(EmptySiteException::new);
    }

    /**
     * Checks to see if braiins.
     *
     * @param values      The values.
     * @param braiinsType The resulting type, if found.
     *
     * @return Whether or not bOS.
     */
    private static boolean isBraiins(
            final Map<String, List<Map<String, String>>> values,
            final AtomicReference<String> braiinsType) {
        final AtomicBoolean isBraiins = new AtomicBoolean(false);
        values
                .entrySet()
                .stream()
                .filter(stringListEntry -> "VERSION".equals(stringListEntry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .forEach(entry -> {
                    final String key = entry.getKey().toLowerCase();
                    final String value = entry.getValue().toLowerCase();
                    isBraiins.compareAndSet(
                            false,
                            key.contains("bos") || value.contains("bos"));
                    if ("type".equalsIgnoreCase(key)) {
                        braiinsType.set(value);
                    }
                });
        return isBraiins.get();
    }

    /**
     * Checks to see if the type is known.
     *
     * @param values The values to inspect.
     *
     * @return The type.
     */
    private static boolean isKnown(final Map<String, String> values) {
        return values.containsKey(TYPE);
    }

    /**
     * Finds the {@link AntminerType} from the provided versions response.
     *
     * @param versionValues The response values.
     * @param hashRateIdeal The ideal hash rate.
     * @param callback      The callback.
     *
     * @return The type.
     *
     * @throws EmptySiteException if type couldn't be determined.
     */
    private static Optional<AntminerType> toType(
            final Map<String, List<Map<String, String>>> versionValues,
            final AtomicReference<String> hashRateIdeal,
            final TypeCallback callback)
            throws EmptySiteException {
        final Map<String, String> values = toVersion(versionValues);
        final String finalType = values.get(TYPE);
        final Optional<AntminerType> type =
                AntminerType.forModel(
                        TYPE,
                        finalType,
                        hashRateIdeal.get());
        callback.accept(
                values.getOrDefault("Type", ""),
                values.getOrDefault("CompileTime", ""),
                finalType);
        return type;
    }

    /** A mechanism for notifying the listener that a type was determined. */
    public interface TypeCallback {

        /**
         * Provides the version type, compile time, and sanitized type.
         *
         * @param versionType        The version type.
         * @param versionCompileTime The compile time.
         * @param sanitizedType      The sanitized type.
         */
        void accept(
                String versionType,
                String versionCompileTime,
                String sanitizedType);
    }
}
