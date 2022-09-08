package mn.foreman.whatsminer;

import mn.foreman.cgminer.CgMiner;
import mn.foreman.cgminer.Context;
import mn.foreman.cgminer.request.CgMinerCommand;
import mn.foreman.cgminer.request.CgMinerRequest;
import mn.foreman.model.*;
import mn.foreman.model.error.MinerException;
import mn.foreman.util.ArgUtils;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** A strategy for detecting Whatsminer miners. */
public class WhatsminerDetectionStrategy
        implements DetectionStrategy {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(WhatsminerDetectionStrategy.class);

    /** The configuration. */
    private final ApplicationConfiguration applicationConfiguration;

    /** The strategy. */
    private final MacStrategy macStrategy;

    /** The miner. */
    private final Miner miner;

    /**
     * Constructor.
     *
     * @param macStrategy              The strategy.
     * @param miner                    The miner.
     * @param applicationConfiguration The configuration.
     */
    public WhatsminerDetectionStrategy(
            final MacStrategy macStrategy,
            final Miner miner,
            final ApplicationConfiguration applicationConfiguration) {
        this.macStrategy = macStrategy;
        this.miner = miner;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    public Optional<Detection> detect(
            final String ip,
            final int port,
            final Map<String, Object> args) {
        Detection detection = null;

        final AtomicReference<WhatsminerType> typeRef =
                new AtomicReference<>();
        try {
            final AtomicBoolean foundCgminer = new AtomicBoolean(false);
            final CgMiner cgMiner =
                    new CgMiner.Builder(new Context(), Collections.emptyList())
                            .setApiIp(ip)
                            .setApiPort(port == 4029 ? port : 4028)
                            .setConnectTimeout(
                                    this.applicationConfiguration.getReadSocketTimeout())
                            .addRequest(
                                    new CgMinerRequest.Builder()
                                            .setCommand(CgMinerCommand.POOLS)
                                            .build(),
                                    (builder, response) -> foundCgminer.set(response != null))
                            .build();
            cgMiner.getStats();

            if (foundCgminer.get()) {
                // Attempt detection with new model devdetails
                Optional<WhatsminerType> type =
                        runNewDetect(
                                ip,
                                port);
                if (!type.isPresent()) {
                    type =
                            runOldDetect(
                                    ip,
                                    args);
                }
                type.ifPresent(typeRef::set);
            }
        } catch (final MinerException me) {
            LOG.debug("No miner found on {}:{}", ip, port, me);
        }

        final WhatsminerType whatsminerType =
                typeRef.get();
        if (whatsminerType != null) {
            final Map<String, Object> newArgs = new HashMap<>(args);
            this.macStrategy.getMacAddress()
                    .ifPresent(mac -> newArgs.put("mac", mac));

            if (ArgUtils.isWorkerPreferred(newArgs)) {
                DetectionUtils.addWorkerFromStats(
                        this.miner,
                        newArgs);
            }

            detection =
                    Detection
                            .builder()
                            .ipAddress(ip)
                            .port(port)
                            .minerType(whatsminerType)
                            .parameters(newArgs)
                            .build();
        }

        return Optional.ofNullable(detection);
    }

    /**
     * Runs a devdetails-based detection.
     *
     * @param ip   The IP.
     * @param port The port.
     *
     * @return The type, if found.
     */
    private Optional<WhatsminerType> runNewDetect(
            final String ip,
            final int port) {
        final AtomicReference<WhatsminerType> typeRef = new AtomicReference<>();
        final CgMiner cgMiner =
                new CgMiner.Builder(new Context(), Collections.emptyList())
                        .setApiIp(ip)
                        .setApiPort(port == 4029 ? port : 4028)
                        .setConnectTimeout(
                                this.applicationConfiguration.getReadSocketTimeout())
                        .addRequest(
                                new CgMinerRequest.Builder()
                                        .setCommand(CgMinerCommand.DEVDETAILS)
                                        .build(),
                                (builder, response) ->
                                        response
                                                .getValues()
                                                .entrySet()
                                                .stream()
                                                .filter(entry -> "DEVDETAILS".equals(entry.getKey()))
                                                .map(Map.Entry::getValue)
                                                .flatMap(List::stream)
                                                .filter(map -> map.containsKey("Model"))
                                                .map(map -> map.get("Model"))
                                                .map(s -> s.split("V"))
                                                .filter(a -> a.length > 1)
                                                .map(a -> a[0])
                                                .findFirst()
                                                .flatMap(WhatsminerType::fromVersion)
                                                .ifPresent(typeRef::set))
                        .build();
        try {
            cgMiner.getStats();
        } catch (final MinerException me) {
            // Ignore
        }
        return Optional.ofNullable(typeRef.get());
    }

    /**
     * Runs a web-based detection.
     *
     * @param ip   The IP.
     * @param args The args.
     *
     * @return The type, if found.
     */
    private Optional<WhatsminerType> runOldDetect(
            final String ip,
            final Map<String, Object> args) {
        final AtomicReference<WhatsminerType> typeRef = new AtomicReference<>();
        try {
            WhatsminerQuery.query(
                    ip,
                    Integer.parseInt(args.getOrDefault("webPort", "443").toString()),
                    args.getOrDefault("username", "").toString(),
                    args.getOrDefault("password", "").toString(),
                    Collections.singletonList(
                            WhatsminerQuery.Query
                                    .builder()
                                    .uri("/cgi-bin/luci/admin/status/overview")
                                    .isGet(true)
                                    .isMultipartForm(false)
                                    .urlParams(Collections.emptyList())
                                    .callback((statusCode, data) -> {
                                        if (statusCode == HttpStatus.SC_OK) {
                                            final int versionStart = data.indexOf("WhatsMiner M");
                                            final String version =
                                                    data.substring(
                                                            versionStart,
                                                            data.indexOf(
                                                                    "<",
                                                                    versionStart));
                                            WhatsminerType.fromVersion(version)
                                                    .ifPresent(typeRef::set);
                                        }
                                    })
                                    .build()),
                    this.applicationConfiguration.getReadSocketTimeout());
        } catch (final MinerException me) {
            // Ignore
        }
        return Optional.ofNullable(typeRef.get());
    }
}