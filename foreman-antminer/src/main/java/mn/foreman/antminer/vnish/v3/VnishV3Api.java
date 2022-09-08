package mn.foreman.antminer.vnish.v3;

import mn.foreman.http.HttpRequestBuilder;
import mn.foreman.http.QueryException;
import mn.foreman.model.ApplicationConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import lombok.Data;
import org.apache.http.HttpStatus;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * The {@link VnishV3Api} provides a mechanism to communicate with a miner
 * running vnish.
 */
public class VnishV3Api {

    /** The mapper for processing JSON. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Returns the autotune logs.
     *
     * @param context The context.
     *
     * @return The system logs.
     */
    public static Optional<String> autotuneLogs(final Context context) {
        return get(
                context.ip,
                context.port,
                context.token,
                context.configuration.getReadSocketTimeout(),
                "/api/v1/logs/autotune",
                (code, body) -> body,
                s -> s);
    }

    /**
     * Changes the pools.
     *
     * @param context The authentication context.
     * @param pools   The new pools.
     *
     * @return The result.
     *
     * @throws QueryException          on failure.
     * @throws JsonProcessingException on failure.
     */
    public static Optional<Boolean> changePools(
            final Context context,
            final List<mn.foreman.api.model.Pool> pools)
            throws QueryException, JsonProcessingException {
        final List<Map<String, Object>> newPools = new LinkedList<>();
        for (int i = 0; i < pools.size(); i++) {
            final mn.foreman.api.model.Pool pool = pools.get(i);
            newPools.add(
                    ImmutableMap.of(
                            "url",
                            pool.getUrl(),
                            "user",
                            pool.getUsername(),
                            "pass",
                            pool.getPassword(),
                            "order",
                            i));
        }
        return post(
                context,
                "/api/v1/settings",
                OBJECT_MAPPER.writeValueAsString(
                        ImmutableMap.of(
                                "miner",
                                ImmutableMap.of(
                                        "pools",
                                        newPools))),
                (code, body) -> true,
                response -> true);
    }

    /**
     * Login to the miner.
     *
     * @param ip            The IP.
     * @param port          The port.
     * @param password      The password.
     * @param configuration The configuration.
     *
     * @return The authentication context.
     *
     * @throws JsonProcessingException on failure to parse JSON.
     * @throws QueryException          on failure to query.
     */
    public static Optional<Context> login(
            final String ip,
            final int port,
            final String password,
            final ApplicationConfiguration configuration) throws JsonProcessingException, QueryException {
        return new HttpRequestBuilder<Map<String, Object>>()
                .scheme("http")
                .ip(ip)
                .port(port)
                .uri("/api/v1/unlock")
                .socketTimeout(
                        configuration.getReadSocketTimeout().getTimeout(),
                        configuration.getReadSocketTimeout().getTimeoutUnits())
                .validator((code, s) -> code == HttpStatus.SC_OK)
                .responseTransformer((code, body) ->
                        OBJECT_MAPPER.readValue(
                                body,
                                new TypeReference<Map<String, Object>>() {
                                }))
                .postJsonWithResponse(
                        OBJECT_MAPPER.writeValueAsString(
                                ImmutableMap.of(
                                        "pw",
                                        password)))
                .map(response ->
                        Context
                                .builder()
                                .configuration(configuration)
                                .token(response.get("token").toString())
                                .ip(ip)
                                .port(port)
                                .build());
    }

    /**
     * Returns the miner logs.
     *
     * @param context The context.
     *
     * @return The system logs.
     */
    public static Optional<String> minerLogs(final Context context) {
        return get(
                context.ip,
                context.port,
                context.token,
                context.configuration.getReadSocketTimeout(),
                "/api/v1/logs/miner",
                (code, body) -> body,
                s -> s);
    }

    /**
     * Reboots the miner.
     *
     * @param context The authentication context.
     *
     * @return The result.
     *
     * @throws QueryException on failure.
     */
    public static Optional<Boolean> reboot(final Context context) throws QueryException {
        return post(
                context,
                "/api/v1/system/reboot",
                null,
                (code, body) -> true,
                response -> true);
    }


    /**
     * Returns the summary.
     *
     * @param context The context.
     *
     * @return The summary.
     *
     * @throws QueryException on failure.
     */
    public static Optional<Summary> summary(final Context context) throws QueryException {
        return get(
                context.ip,
                context.port,
                context.token,
                context.configuration.getReadSocketTimeout(),
                "/api/v1/summary",
                (code, body) -> OBJECT_MAPPER.readValue(body, Summary.class),
                summary -> summary);
    }

    /**
     * Returns the system logs.
     *
     * @param context The context.
     *
     * @return The system logs.
     */
    public static Optional<String> systemLogs(final Context context) {
        return get(
                context.ip,
                context.port,
                context.token,
                context.configuration.getReadSocketTimeout(),
                "/api/v1/logs/system",
                (code, body) -> body,
                s -> s);
    }

    /**
     * Performs a GET operation against the API.
     *
     * @param ip          The ip.
     * @param port        The port.
     * @param token       The password.
     * @param timeConfig  The configuration.
     * @param uri         The URI.
     * @param transformer The response transformer.
     * @param mapper      The mapper for parsing the response.
     * @param <U>         The response type.
     * @param <T>         The result type.
     *
     * @return The result.
     */
    private static <U, T> Optional<T> get(
            final String ip,
            final int port,
            final String token,
            final ApplicationConfiguration.TimeConfig timeConfig,
            final String uri,
            final HttpRequestBuilder.ResponseTransformer<U> transformer,
            final Function<U, T> mapper) {
        final HttpRequestBuilder<U> builder =
                new HttpRequestBuilder<U>()
                        .scheme("http")
                        .ip(ip)
                        .port(port)
                        .uri(uri)
                        .socketTimeout(
                                timeConfig.getTimeout(),
                                timeConfig.getTimeoutUnits())
                        .validator((code, s) -> code == HttpStatus.SC_OK)
                        .responseTransformer(transformer);
        if (token != null) {
            builder.header("authorization", "Bearer " + token);
        }
        return builder
                .get()
                .map(mapper);
    }

    /**
     * Performs a POST operation against the API.
     *
     * @param context     The authentication context.
     * @param uri         The URI.
     * @param content     The request content.
     * @param transformer The response transformer.
     * @param mapper      The mapper for parsing the response.
     * @param <U>         The response type.
     * @param <T>         The result type.
     *
     * @return The result.
     *
     * @throws QueryException on failure.
     */
    private static <U, T> Optional<T> post(
            final Context context,
            final String uri,
            final String content,
            final HttpRequestBuilder.ResponseTransformer<U> transformer,
            final Function<U, T> mapper) throws QueryException {
        final ApplicationConfiguration.TimeConfig timeConfig =
                context.configuration.getWriteSocketTimeout();
        return new HttpRequestBuilder<U>()
                .scheme("http")
                .ip(context.ip)
                .port(context.port)
                .header("authorization", "Bearer " + context.token)
                .uri(uri)
                .socketTimeout(
                        timeConfig.getTimeout(),
                        timeConfig.getTimeoutUnits())
                .validator((code, s) -> code == HttpStatus.SC_OK)
                .responseTransformer(transformer)
                .postJsonWithResponse(content)
                .map(mapper);
    }

    /** A auth miner context. */
    @Data
    @Builder
    public static class Context {

        /** The configuration. */
        public ApplicationConfiguration configuration;

        /** The IP. */
        public String ip;

        /** The port. */
        public int port;

        /** The bearer token. */
        public String token;
    }

    /** A summary object representation. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Summary {

        /** The miner. */
        @JsonProperty("miner")
        public Miner miner;

        /** The system. */
        @JsonProperty("system")
        public System system;

        /** A miner object representation. */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Miner {

            /** The chains. */
            @JsonProperty("chains")
            public List<Chain> chains;

            /** The compile time. */
            @JsonProperty("compile_time")
            public String compileTime;

            /** The cooling. */
            @JsonProperty("cooling")
            public Cooling cooling;

            /** The hash rate. */
            @JsonProperty("instant_hashrate")
            public String instantHashrate;

            /** The miner type. */
            @JsonProperty("miner_type")
            public String minerType;

            /** The pools. */
            @JsonProperty("pools")
            public List<Pool> pools;

            /** The power usage. */
            @JsonProperty("power_usage")
            public double powerUsage;

            /** The status. */
            @JsonProperty("miner_status")
            public MinerStatus status;

            /** The chain. */
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Chain {

                /** The chip temp sensors. */
                @JsonProperty("chip_temp_sens")
                public List<Sensor> chipTempSensor;

                /** The real time hash rate. */
                @JsonProperty("hashrate_rt")
                public double hashrateRt;

                /** The pcb temp sensor. */
                @JsonProperty("pcb_temp_sens")
                public List<Sensor> pcbTempSensor;

                /** The sensor. */
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Sensor {

                    /** The temperature. */
                    @JsonProperty("temp")
                    public int temp;
                }
            }

            /** The cooling. */
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Cooling {

                /** The fan number. */
                @JsonProperty("fan_num")
                public int fanNum;

                /** The fans. */
                @JsonProperty("fans")
                public List<Fan> fans;

                /** The settings. */
                @JsonProperty("settings")
                public Settings settings;

                /** The fan. */
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Fan {

                    /** The RPM. */
                    @JsonProperty("rpm")
                    public int rpm;
                }

                /** The settings. */
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Settings {

                    /** The mode. */
                    @JsonProperty("mode")
                    public Mode mode;

                    /** The mode. */
                    @JsonIgnoreProperties(ignoreUnknown = true)
                    public static class Mode {

                        /** The name. */
                        @JsonProperty("name")
                        public String name;
                    }
                }
            }

            /** A single pool. */
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Pool {

                /** The accepted shares. */
                @JsonProperty("accepted")
                public int accepted;

                /** The difficulty. */
                @JsonProperty("diff")
                public String difficulty;

                /** The difficulty accepted. */
                @JsonProperty("diffa")
                public BigInteger difficultyAccepted;

                /** The last share difficulty. */
                @JsonProperty("ls_diff")
                public int lastShareDifficulty;

                /** The ID. */
                @JsonProperty("id")
                public int priority;

                /** The rejected shares. */
                @JsonProperty("rejected")
                public int rejected;

                /** The stale shares. */
                @JsonProperty("stale")
                public int stale;

                /** The status. */
                @JsonProperty("status")
                public String status;

                /** The URL. */
                @JsonProperty("url")
                public String url;

                /** The username. */
                @JsonProperty("user")
                public String worker;
            }

            /** The miner status. */
            @JsonIgnoreProperties(ignoreUnknown = true)
            public class MinerStatus {

                /** The state. */
                @JsonProperty("miner_state")
                public String state;
            }
        }

        /** A system object representation. */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class System {

            /** The network. */
            @JsonProperty("network_status")
            public Network network;

            /** A network object representation. */
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Network {

                /** The mac. */
                @JsonProperty("mac")
                public String mac;
            }
        }
    }
}