package mn.foreman.io;

import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.error.MinerException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Provides utility methods for querying APIs. */
public class Query {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(Query.class);

    /** The mapper for JSON. */
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule());

    /**
     * Constructor.
     *
     * Note: intentionally hidden.
     */
    private Query() {
        // Do nothing
    }

    /**
     * Utility method to perform a query against a delimiter-based API.
     *
     * @param apiIp   The API IP.
     * @param apiPort The API port.
     * @param command The command.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    public static String delimiterQuery(
            final String apiIp,
            final int apiPort,
            final String command)
            throws MinerException {
        return delimiterQuery(
                apiIp,
                apiPort,
                command,
                15,
                TimeUnit.SECONDS);
    }

    /**
     * Utility method to perform a query against a delimiter-based API.
     *
     * @param apiIp               The API IP.
     * @param apiPort             The API port.
     * @param command             The command.
     * @param connectTimeout      The connection timeout.
     * @param connectTimeoutUnits The connection timeout units.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    public static String delimiterQuery(
            final String apiIp,
            final int apiPort,
            final String command,
            final int connectTimeout,
            final TimeUnit connectTimeoutUnits)
            throws MinerException {
        final ApiRequest request =
                new ApiRequestImpl(
                        apiIp,
                        apiPort,
                        command);
        final Connection connection =
                ConnectionFactory.createDelimiterConnection(
                        request,
                        connectTimeout,
                        connectTimeoutUnits);
        connection.query();

        final boolean completed =
                request.waitForCompletion(
                        connectTimeout,
                        connectTimeoutUnits);
        final String response =
                request.getResponse();
        if (!completed || response == null) {
            throw new MinerException("Failed to obtain a response");
        }

        return response;
    }

    /**
     * Performs an HTTP GET operation against an API that requires digest auth.
     *
     * @param host              The host.
     * @param port              The port.
     * @param realm             The realm.
     * @param path              The path.
     * @param username          The digest auth username.
     * @param password          The digest auth password.
     * @param content           The GET content.
     * @param responseProcessor The response processor.
     * @param timeConfig        The configuration.
     *
     * @throws Exception on failure to connect.
     */
    public static void digestGet(
            final String host,
            final int port,
            final String realm,
            final String path,
            final String username,
            final String password,
            final List<Map<String, Object>> content,
            final BiConsumer<Integer, String> responseProcessor,
            final ApplicationConfiguration.TimeConfig timeConfig)
            throws Exception {
        doDigest(
                host,
                port,
                realm,
                path,
                username,
                password,
                false,
                content,
                null,
                false,
                responseProcessor,
                timeConfig.getTimeout(),
                timeConfig.getTimeoutUnits());
    }

    /**
     * Performs an HTTP GET operation against an API that requires digest auth.
     *
     * @param host              The host.
     * @param port              The port.
     * @param realm             The realm.
     * @param path              The path.
     * @param username          The digest auth username.
     * @param password          The digest auth password.
     * @param content           The GET content.
     * @param responseProcessor The response processor.
     *
     * @throws Exception on failure to connect.
     */
    public static void digestGet(
            final String host,
            final int port,
            final String realm,
            final String path,
            final String username,
            final String password,
            final List<Map<String, Object>> content,
            final BiConsumer<Integer, String> responseProcessor)
            throws Exception {
        doDigest(
                host,
                port,
                realm,
                path,
                username,
                password,
                false,
                content,
                null,
                false,
                responseProcessor,
                5,
                TimeUnit.SECONDS);
    }

    /**
     * Performs an HTTP GET operation against an API that requires digest auth.
     *
     * @param host              The host.
     * @param port              The port.
     * @param realm             The realm.
     * @param path              The path.
     * @param username          The digest auth username.
     * @param password          The digest auth password.
     * @param responseProcessor The response processor.
     * @param timeConfig        The configuration.
     *
     * @throws Exception on failure to connect.
     */
    public static void digestGet(
            final String host,
            final int port,
            final String realm,
            final String path,
            final String username,
            final String password,
            final BiConsumer<Integer, String> responseProcessor,
            final ApplicationConfiguration.TimeConfig timeConfig)
            throws Exception {
        doDigest(
                host,
                port,
                realm,
                path,
                username,
                password,
                false,
                null,
                null,
                false,
                responseProcessor,
                timeConfig.getTimeout(),
                timeConfig.getTimeoutUnits());
    }

    /**
     * Performs an HTTP GET operation against an API that requires digest auth.
     *
     * @param host              The host.
     * @param port              The port.
     * @param realm             The realm.
     * @param path              The path.
     * @param username          The digest auth username.
     * @param password          The digest auth password.
     * @param responseProcessor The response processor.
     *
     * @throws Exception on failure to connect.
     */
    public static void digestGet(
            final String host,
            final int port,
            final String realm,
            final String path,
            final String username,
            final String password,
            final BiConsumer<Integer, String> responseProcessor,
            final int socketTimeout,
            final TimeUnit socketTimeoutUnits)
            throws Exception {
        doDigest(
                host,
                port,
                realm,
                path,
                username,
                password,
                false,
                null,
                null,
                false,
                responseProcessor,
                socketTimeout,
                socketTimeoutUnits);
    }

    /**
     * Performs an HTTP GET operation against an API that requires digest auth.
     *
     * @param host              The host.
     * @param port              The port.
     * @param realm             The realm.
     * @param path              The path.
     * @param username          The digest auth username.
     * @param password          The digest auth password.
     * @param responseProcessor The response processor.
     *
     * @throws Exception on failure to connect.
     */
    public static void digestGet(
            final String host,
            final int port,
            final String realm,
            final String path,
            final String username,
            final String password,
            final BiConsumer<Integer, String> responseProcessor)
            throws Exception {
        digestGet(
                host,
                port,
                realm,
                path,
                username,
                password,
                responseProcessor,
                1,
                TimeUnit.SECONDS);
    }

    /**
     * Performs an HTTP post operation.
     *
     * @param host              The host.
     * @param port              The port.
     * @param realm             The realm.
     * @param path              The path.
     * @param username          The username.
     * @param password          The password.
     * @param content           The content.
     * @param payload           The payload.
     * @param responseProcessor The response processor.
     * @param timeConfig        The configuration.
     *
     * @throws Exception on failure to connect.
     */
    public static void digestPost(
            final String host,
            final int port,
            final String realm,
            final String path,
            final String username,
            final String password,
            final List<Map<String, Object>> content,
            final String payload,
            final BiConsumer<Integer, String> responseProcessor,
            final ApplicationConfiguration.TimeConfig timeConfig)
            throws Exception {
        doDigest(
                host,
                port,
                realm,
                path,
                username,
                password,
                true,
                content,
                payload,
                false,
                responseProcessor,
                timeConfig.getTimeout(),
                timeConfig.getTimeoutUnits());
    }

    /**
     * Performs an HTTP post operation.
     *
     * @param host              The host.
     * @param port              The port.
     * @param realm             The realm.
     * @param path              The path.
     * @param username          The username.
     * @param password          The password.
     * @param content           The content.
     * @param payload           The payload.
     * @param multipart         Whether multipart.
     * @param responseProcessor The response processor.
     * @param timeout           The socket timeout.
     * @param timeoutUnits      The socket timeout (units).
     *
     * @throws Exception on failure to connect.
     */
    public static void digestPost(
            final String host,
            final int port,
            final String realm,
            final String path,
            final String username,
            final String password,
            final List<Map<String, Object>> content,
            final String payload,
            final boolean multipart,
            final BiConsumer<Integer, String> responseProcessor,
            final int timeout,
            final TimeUnit timeoutUnits)
            throws Exception {
        doDigest(
                host,
                port,
                realm,
                path,
                username,
                password,
                true,
                content,
                payload,
                multipart,
                responseProcessor,
                timeout,
                timeoutUnits);
    }

    /**
     * Performs an HTTP post operation.
     *
     * @param host              The host.
     * @param port              The port.
     * @param realm             The realm.
     * @param path              The path.
     * @param username          The username.
     * @param password          The password.
     * @param content           The content.
     * @param payload           The payload.
     * @param responseProcessor The response processor.
     *
     * @throws Exception on failure to connect.
     */
    public static void digestPost(
            final String host,
            final int port,
            final String realm,
            final String path,
            final String username,
            final String password,
            final List<Map<String, Object>> content,
            final String payload,
            final BiConsumer<Integer, String> responseProcessor)
            throws Exception {
        doDigest(
                host,
                port,
                realm,
                path,
                username,
                password,
                true,
                content,
                payload,
                false,
                responseProcessor,
                20,
                TimeUnit.SECONDS);
    }

    /**
     * Utility method to perform a query against a JSON RPC API.
     *
     * @param apiIp   The API IP.
     * @param apiPort The API port.
     * @param command The command.
     * @param type    The response class.
     * @param <T>     The response type.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    public static <T> T jsonQuery(
            final String apiIp,
            final int apiPort,
            final String command,
            final TypeReference<T> type)
            throws MinerException {
        return jsonQuery(
                apiIp,
                apiPort,
                command,
                type,
                10,
                TimeUnit.SECONDS);
    }

    /**
     * Utility method to perform a query against a JSON RPC API.
     *
     * @param apiIp               The API IP.
     * @param apiPort             The API port.
     * @param command             The command.
     * @param type                The response class.
     * @param connectTimeout      The connection timeout.
     * @param connectTimeoutUnits The connection timeout units.
     * @param <T>                 The response type.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    public static <T> T jsonQuery(
            final String apiIp,
            final int apiPort,
            final String command,
            final TypeReference<T> type,
            final int connectTimeout,
            final TimeUnit connectTimeoutUnits)
            throws MinerException {
        final ApiRequest request =
                new ApiRequestImpl(
                        apiIp,
                        apiPort,
                        command);
        final Connection connection =
                ConnectionFactory.createJsonConnection(
                        request,
                        connectTimeout,
                        connectTimeoutUnits);
        connection.query();

        return query(
                request,
                type,
                connectTimeout,
                connectTimeoutUnits,
                s -> {
                });
    }

    /**
     * Performs a POST with content.
     *
     * @param host              The host.
     * @param port              The port.
     * @param path              The path.
     * @param content           The content.
     * @param responseProcessor The response processor.
     * @param timeConfig        The configuration.
     *
     * @throws Exception on failure.
     */
    public static void post(
            final String host,
            final int port,
            final String path,
            final List<Map<String, Object>> content,
            final BiConsumer<Integer, String> responseProcessor,
            final ApplicationConfiguration.TimeConfig timeConfig)
            throws Exception {
        doDigest(
                host,
                port,
                null,
                path,
                null,
                null,
                true,
                content,
                null,
                false,
                responseProcessor,
                timeConfig.getTimeout(),
                timeConfig.getTimeoutUnits());
    }

    /**
     * Performs a POST with content.
     *
     * @param host              The host.
     * @param port              The port.
     * @param path              The path.
     * @param content           The content.
     * @param responseProcessor The response processor.
     *
     * @throws Exception on failure.
     */
    public static void post(
            final String host,
            final int port,
            final String path,
            final List<Map<String, Object>> content,
            final BiConsumer<Integer, String> responseProcessor,
            final int timeout,
            final TimeUnit timeoutUnits)
            throws Exception {
        doDigest(
                host,
                port,
                null,
                path,
                null,
                null,
                true,
                content,
                null,
                false,
                responseProcessor,
                timeout,
                timeoutUnits);
    }

    /**
     * Performs a POST with content.
     *
     * @param host              The host.
     * @param port              The port.
     * @param path              The path.
     * @param content           The content.
     * @param responseProcessor The response processor.
     *
     * @throws Exception on failure.
     */
    public static void post(
            final String host,
            final int port,
            final String path,
            final List<Map<String, Object>> content,
            final BiConsumer<Integer, String> responseProcessor)
            throws Exception {
        post(
                host,
                port,
                path,
                content,
                responseProcessor,
                20,
                TimeUnit.SECONDS);
    }

    /**
     * Performs a POST with content.
     *
     * @param host              The host.
     * @param port              The port.
     * @param path              The path.
     * @param content           The content.
     * @param payload           The payload.
     * @param responseProcessor The response processor.
     *
     * @throws Exception on failure.
     */
    public static void post(
            final String host,
            final int port,
            final String path,
            final List<Map<String, Object>> content,
            final String payload,
            final BiConsumer<Integer, String> responseProcessor)
            throws Exception {
        doDigest(
                host,
                port,
                null,
                path,
                null,
                null,
                true,
                content,
                payload,
                false,
                responseProcessor,
                20,
                TimeUnit.SECONDS);
    }

    /**
     * Performs a POST with content.
     *
     * @param host              The host.
     * @param port              The port.
     * @param path              The path.
     * @param auth              The auth.
     * @param payload           The payload.
     * @param type              The type.
     * @param timeConfig        The config.
     * @param responseProcessor The response processor.
     * @param <T>               The type.
     *
     * @throws Exception on failure.
     */
    public static <T> Optional<T> restPost(
            final String host,
            final int port,
            final String path,
            final String auth,
            final String payload,
            final TypeReference<T> type,
            final ApplicationConfiguration.TimeConfig timeConfig,
            final BiConsumer<Integer, String> responseProcessor)
            throws Exception {
        return restPost(
                host,
                port,
                path,
                auth,
                payload,
                type,
                timeConfig.getTimeout(),
                timeConfig.getTimeoutUnits(),
                responseProcessor);
    }

    /**
     * Performs a POST with content.
     *
     * @param host              The host.
     * @param port              The port.
     * @param path              The path.
     * @param auth              The auth.
     * @param payload           The payload.
     * @param type              The type.
     * @param timeout           The timeout.
     * @param timeoutUnits      The timeout (units).
     * @param responseProcessor The response processor.
     * @param <T>               The type.
     *
     * @throws Exception on failure.
     */
    public static <T> Optional<T> restPost(
            final String host,
            final int port,
            final String path,
            final String auth,
            final String payload,
            final TypeReference<T> type,
            final int timeout,
            final TimeUnit timeoutUnits,
            final BiConsumer<Integer, String> responseProcessor)
            throws Exception {
        T result;

        final URI uri =
                new URI(
                        "http",
                        null,
                        host,
                        port,
                        path,
                        null,
                        null);
        final URL url = uri.toURL();

        final HttpHost targetHost =
                new HttpHost(
                        url.getHost(),
                        url.getPort(),
                        url.getProtocol());

        CloseableHttpClient httpClient = null;
        try {
            final HttpClientContext context = HttpClientContext.create();
            httpClient =
                    HttpClients
                            .custom()
                            .disableAutomaticRetries()
                            .setDefaultRequestConfig(
                                    RequestConfig
                                            .custom()
                                            .setConnectTimeout((int) timeoutUnits.toMillis(timeout))
                                            .setSocketTimeout((int) timeoutUnits.toMillis(timeout))
                                            .build())
                            .build();

            final HttpPost httpPost = new HttpPost(url.getPath());
            httpPost.setEntity(new StringEntity(payload));
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            if (auth != null) {
                httpPost.setHeader("Authorization", "Bearer " + auth);
            }

            LOG.debug("Sending request: {}", httpPost);

            try (final CloseableHttpResponse response =
                         httpClient.execute(
                                 targetHost,
                                 httpPost,
                                 context)) {
                final StatusLine statusLine =
                        response.getStatusLine();
                final String responseBody =
                        EntityUtils.toString(response.getEntity());
                LOG.debug("Received digest API response: {}", responseBody);
                try {
                    result =
                            OBJECT_MAPPER.readValue(
                                    responseBody,
                                    type);
                } catch (final Exception e) {
                    throw new MinerException(e);
                }
                responseProcessor.accept(
                        statusLine.getStatusCode(),
                        responseBody);
            }
        } finally {
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return Optional.ofNullable(result);
    }

    /**
     * Utility method to perform a query against a REST API.
     *
     * @param apiIp                  The API IP.
     * @param apiPort                The API port.
     * @param uri                    The URI.
     * @param username               The username.
     * @param password               The password.
     * @param type                   The response class.
     * @param <T>                    The response type.
     * @param connectionTimeout      The timeout.
     * @param connectionTimeoutUnits The units.
     * @param rawCallback            The raw callback.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    public static <T> T restQuery(
            final String apiIp,
            final int apiPort,
            final String uri,
            final String username,
            final String password,
            final TypeReference<T> type,
            final int connectionTimeout,
            final TimeUnit connectionTimeoutUnits,
            final Consumer<String> rawCallback)
            throws MinerException {
        final String auth =
                Base64.getEncoder().encodeToString(
                        (username + ":" + password).getBytes());
        return restQuery(
                apiIp,
                apiPort,
                uri,
                ImmutableMap.of(
                        "Authorization",
                        "Basic " + auth),
                "POST",
                type,
                connectionTimeout,
                connectionTimeoutUnits,
                rawCallback);
    }

    /**
     * Utility method to perform a query against a REST API.
     *
     * @param apiIp       The API IP.
     * @param apiPort     The API port.
     * @param uri         The URI.
     * @param username    The username.
     * @param password    The password.
     * @param type        The response class.
     * @param rawCallback The raw callback.
     * @param timeConfig  The configuration.
     * @param <T>         The response type.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    public static <T> T restQuery(
            final String apiIp,
            final int apiPort,
            final String uri,
            final String username,
            final String password,
            final TypeReference<T> type,
            final Consumer<String> rawCallback,
            final ApplicationConfiguration.TimeConfig timeConfig)
            throws MinerException {
        return restQuery(
                apiIp,
                apiPort,
                uri,
                username,
                password,
                type,
                timeConfig.getTimeout(),
                timeConfig.getTimeoutUnits(),
                rawCallback);
    }

    /**
     * Utility method to perform a query against a REST API.
     *
     * @param apiIp               The API IP.
     * @param apiPort             The API port.
     * @param uri                 The URI.
     * @param headers             The headers.
     * @param command             The command.
     * @param type                The response class.
     * @param connectTimeout      The connection timeout.
     * @param connectTimeoutUnits The connection timeout units.
     * @param rawCallback         The raw callback.
     * @param <T>                 The response type.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    public static <T> T restQuery(
            final String apiIp,
            final int apiPort,
            final String uri,
            final Map<String, String> headers,
            final String command,
            final TypeReference<T> type,
            final int connectTimeout,
            final TimeUnit connectTimeoutUnits,
            final Consumer<String> rawCallback)
            throws MinerException {
        final ApiRequest request =
                new ApiRequestImpl(
                        apiIp,
                        apiPort,
                        uri,
                        headers);

        final Connection connection =
                ConnectionFactory.createRestConnection(
                        request,
                        command,
                        connectTimeout,
                        connectTimeoutUnits);
        connection.query();

        return query(
                request,
                type,
                connectTimeout,
                connectTimeoutUnits,
                rawCallback);
    }

    /**
     * Utility method to perform a query against a REST API.
     *
     * @param apiIp   The API IP.
     * @param apiPort The API port.
     * @param uri     The URI.
     * @param type    The response class.
     * @param <T>     The response type.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    public static <T> T restQuery(
            final String apiIp,
            final int apiPort,
            final String uri,
            final TypeReference<T> type)
            throws MinerException {
        return restQuery(
                apiIp,
                apiPort,
                uri,
                "GET",
                type,
                2,
                TimeUnit.SECONDS,
                s -> {
                });
    }

    /**
     * Utility method to perform a query against a REST API.
     *
     * @param apiIp    The API IP.
     * @param apiPort  The API port.
     * @param uri      The URI.
     * @param type     The response class.
     * @param callback The callback.
     * @param <T>      The response type.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    public static <T> T restQuery(
            final String apiIp,
            final int apiPort,
            final String uri,
            final TypeReference<T> type,
            final Consumer<String> callback)
            throws MinerException {
        return restQuery(
                apiIp,
                apiPort,
                uri,
                "GET",
                type,
                2,
                TimeUnit.SECONDS,
                callback);
    }

    /**
     * Utility method to perform a query against a REST API.
     *
     * @param apiIp               The API IP.
     * @param apiPort             The API port.
     * @param uri                 The URI.
     * @param command             The command.
     * @param type                The response class.
     * @param connectTimeout      The connection timeout.
     * @param connectTimeoutUnits The connection timeout units.
     * @param callback            The callback.
     * @param <T>                 The response type.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    public static <T> T restQuery(
            final String apiIp,
            final int apiPort,
            final String uri,
            final String command,
            final TypeReference<T> type,
            final int connectTimeout,
            final TimeUnit connectTimeoutUnits,
            final Consumer<String> callback)
            throws MinerException {
        final ApiRequest request =
                new ApiRequestImpl(
                        apiIp,
                        apiPort,
                        uri);

        final Connection connection =
                ConnectionFactory.createRestConnection(
                        request,
                        command,
                        connectTimeout,
                        connectTimeoutUnits);
        connection.query();

        return query(
                request,
                type,
                connectTimeout,
                connectTimeoutUnits,
                callback);
    }

    /**
     * Performs a rest query with basic auth.
     *
     * @param host              The host.
     * @param port              The port.
     * @param path              The path.
     * @param username          The username.
     * @param password          The password.
     * @param content           The content.
     * @param responseProcessor The processor for responses.
     * @param timeConfig        The socket config.
     *
     * @throws IOException        on failure.
     * @throws URISyntaxException on failure.
     */
    public static void restQuery(
            final String host,
            final int port,
            final String path,
            final String username,
            final String password,
            final List<Map<String, Object>> content,
            final Consumer<String> responseProcessor,
            final ApplicationConfiguration.TimeConfig timeConfig) throws IOException,
            URISyntaxException {
        runRestQuery(
                false,
                host,
                port,
                path,
                username,
                password,
                content,
                responseProcessor,
                timeConfig.getTimeout(),
                timeConfig.getTimeoutUnits());
    }

    /**
     * Utility method to perform a query against a REST API.
     *
     * @param apiIp               The API IP.
     * @param apiPort             The API port.
     * @param uri                 The URI.
     * @param token               The token.
     * @param type                The response class.
     * @param connectTimeout      The connection timeout.
     * @param connectTimeoutUnits The connection timeout units.
     * @param rawCallback         The raw callback.
     * @param <T>                 The response type.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    public static <T> T restQueryBearer(
            final String apiIp,
            final int apiPort,
            final String uri,
            final String token,
            final TypeReference<T> type,
            final int connectTimeout,
            final TimeUnit connectTimeoutUnits,
            final Consumer<String> rawCallback)
            throws MinerException {
        return restQuery(
                apiIp,
                apiPort,
                uri,
                ImmutableMap.of(
                        "Accept",
                        "application/json",
                        "Authorization",
                        "Bearer " + token,
                        "Content-Type",
                        "application/json"),
                "GET",
                type,
                connectTimeout,
                connectTimeoutUnits,
                rawCallback);
    }

    /**
     * Utility method to perform a query against a REST API.
     *
     * @param apiIp      The API IP.
     * @param apiPort    The API port.
     * @param uri        The URI.
     * @param token      The token.
     * @param type       The response class.
     * @param timeConfig The configuration.
     * @param <T>        The response type.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    public static <T> T restQueryBearer(
            final String apiIp,
            final int apiPort,
            final String uri,
            final String token,
            final TypeReference<T> type,
            final ApplicationConfiguration.TimeConfig timeConfig)
            throws MinerException {
        return restQueryBearer(
                apiIp,
                apiPort,
                uri,
                token,
                type,
                timeConfig.getTimeout(),
                timeConfig.getTimeoutUnits(),
                s -> {
                });
    }

    /**
     * Utility method to perform a query against a REST API.
     *
     * @param apiIp      The API IP.
     * @param apiPort    The API port.
     * @param uri        The URI.
     * @param token      The token.
     * @param type       The response class.
     * @param timeConfig The socket config.
     * @param consumer   The callback.
     * @param <T>        The response type.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    public static <T> T restQueryBearer(
            final String apiIp,
            final int apiPort,
            final String uri,
            final String token,
            final TypeReference<T> type,
            final ApplicationConfiguration.TimeConfig timeConfig,
            final Consumer<String> consumer)
            throws MinerException {
        return restQueryBearer(
                apiIp,
                apiPort,
                uri,
                token,
                type,
                timeConfig.getTimeout(),
                timeConfig.getTimeoutUnits(),
                consumer);
    }

    /**
     * Utility method to perform a query against a REST API.
     *
     * @param apiIp               The API IP.
     * @param apiPort             The API port.
     * @param uri                 The URI.
     * @param token               The token.
     * @param type                The response class.
     * @param connectTimeout      The connection timeout.
     * @param connectTimeoutUnits The connection timeout units.
     * @param <T>                 The response type.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    public static <T> T restQueryBearer(
            final String apiIp,
            final int apiPort,
            final String uri,
            final String token,
            final TypeReference<T> type,
            final int connectTimeout,
            final TimeUnit connectTimeoutUnits)
            throws MinerException {
        return restQueryBearer(
                apiIp,
                apiPort,
                uri,
                token,
                type,
                connectTimeout,
                connectTimeoutUnits,
                s -> {
                });
    }

    /**
     * Runs a digest request.
     *
     * @param host               The host.
     * @param port               The port.
     * @param realm              The realm.
     * @param path               The path.
     * @param username           The username.
     * @param password           The password.
     * @param isPost             Whether or not the request is a post.
     * @param content            The content.
     * @param payload            The payload.
     * @param multipart          Whether multipart.
     * @param responseProcessor  What to do with the response.
     * @param socketTimeout      The socket timeout.
     * @param socketTimeoutUnits The socket timeout units.
     *
     * @throws Exception on failure to connect.
     */
    private static void doDigest(
            final String host,
            final int port,
            final String realm,
            final String path,
            final String username,
            final String password,
            final boolean isPost,
            final List<Map<String, Object>> content,
            final String payload,
            final boolean multipart,
            final BiConsumer<Integer, String> responseProcessor,
            final int socketTimeout,
            final TimeUnit socketTimeoutUnits)
            throws Exception {
        final URI uri =
                new URI(
                        "http",
                        null,
                        host,
                        port,
                        path,
                        null,
                        null);
        final URL url = uri.toURL();

        final HttpHost targetHost =
                new HttpHost(
                        url.getHost(),
                        url.getPort(),
                        url.getProtocol());

        final int socketTimeoutMillis =
                (int) socketTimeoutUnits.toMillis(socketTimeout);

        CloseableHttpClient httpClient = null;
        try {
            final HttpClientContext context = HttpClientContext.create();

            if (realm != null && username != null) {
                final CredentialsProvider credsProvider =
                        new BasicCredentialsProvider();
                credsProvider.setCredentials(
                        AuthScope.ANY,
                        new UsernamePasswordCredentials(
                                username,
                                password));
                final AuthCache authCache = new BasicAuthCache();
                final DigestScheme digestScheme = new DigestScheme();
                digestScheme.overrideParamter(
                        "realm",
                        realm);
                digestScheme.overrideParamter(
                        "nonce",
                        UUID
                                .randomUUID()
                                .toString()
                                .replace("-", ""));
                authCache.put(targetHost, digestScheme);

                httpClient =
                        HttpClients
                                .custom()
                                .setDefaultCredentialsProvider(credsProvider)
                                .disableAutomaticRetries()
                                .setDefaultRequestConfig(
                                        RequestConfig
                                                .custom()
                                                .setConnectTimeout(socketTimeoutMillis)
                                                .setSocketTimeout(socketTimeoutMillis)
                                                .build())
                                .build();
                context.setAuthCache(authCache);
            } else {
                httpClient =
                        HttpClients
                                .custom()
                                .disableAutomaticRetries()
                                .setDefaultRequestConfig(
                                        RequestConfig
                                                .custom()
                                                .setConnectTimeout(socketTimeoutMillis)
                                                .setSocketTimeout(socketTimeoutMillis)
                                                .build())
                                .build();
            }

            final HttpRequest httpRequest;
            if (!isPost) {
                // GET
                final URIBuilder uriBuilder =
                        new URIBuilder()
                                .setScheme("http")
                                .setHost(host)
                                .setPort(port)
                                .setPath(path);
                if (content != null) {
                    content.forEach(entry ->
                            uriBuilder.addParameter(
                                    entry.get("key").toString(),
                                    entry.get("value").toString()));
                }
                httpRequest = new HttpGet(uriBuilder.build());
            } else {
                final HttpPost httpPost = new HttpPost(url.getPath());
                if (content != null) {
                    final HttpEntity entity;
                    if (multipart) {
                        final MultipartEntityBuilder builder =
                                MultipartEntityBuilder.create();
                        content.forEach(param -> {
                            final String key = param.get("key").toString();
                            final Object value = param.get("value");
                            if (value instanceof File) {
                                builder.addBinaryBody(
                                        key,
                                        (File) value,
                                        ContentType.APPLICATION_OCTET_STREAM,
                                        "blob");
                            } else if (value instanceof byte[]) {
                                builder.addBinaryBody(
                                        key,
                                        (byte[]) value,
                                        ContentType.APPLICATION_OCTET_STREAM,
                                        "blob");
                            } else {
                                builder.addTextBody(
                                        key,
                                        value.toString());
                            }
                        });
                        entity = builder.build();
                    } else {
                        final List<NameValuePair> params = new ArrayList<>();
                        content.forEach(entry ->
                                params.add(
                                        new BasicNameValuePair(
                                                entry.get("key").toString(),
                                                entry.get("value").toString())));
                        LOG.debug("Params for POST: {}", params);

                        entity =
                                new UrlEncodedFormEntity(
                                        params,
                                        "UTF-8");
                    }
                    LOG.debug("Entity: {}", entity);
                    httpPost.setEntity(entity);
                } else if (payload != null) {
                    httpPost.setEntity(new StringEntity(payload));
                    httpPost.setHeader("Accept", "application/json");
                    httpPost.setHeader("Content-type", "application/json");
                }
                httpRequest = httpPost;
            }

            LOG.debug("Sending request: {}", httpRequest);

            try (final CloseableHttpResponse response =
                         httpClient.execute(
                                 targetHost,
                                 httpRequest,
                                 context)) {
                final StatusLine statusLine =
                        response.getStatusLine();
                final String responseBody =
                        EntityUtils.toString(response.getEntity());
                LOG.debug("Received digest API response: {}", responseBody);
                responseProcessor.accept(
                        statusLine.getStatusCode(),
                        responseBody);
            } catch (final SocketTimeoutException ste) {
                final String message = ste.getMessage();
                if (message != null && !message.contains("Read timed out")) {
                    // Allow read timeouts - sometimes, miners just don't
                    // respond
                    throw ste;
                }
            }
        } finally {
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }

    /**
     * Runs the query.
     *
     * @param request             The request.
     * @param type                The response class.
     * @param connectTimeout      The connection timeout.
     * @param connectTimeoutUnits The connection timeout units.
     * @param rawCallback         The raw callback.
     * @param <T>                 The response type.
     *
     * @return The response.
     *
     * @throws MinerException on failure to query.
     */
    private static <T> T query(
            final ApiRequest request,
            final TypeReference<T> type,
            final int connectTimeout,
            final TimeUnit connectTimeoutUnits,
            final Consumer<String> rawCallback)
            throws MinerException {
        T response;
        if (request.waitForCompletion(
                connectTimeout,
                connectTimeoutUnits)) {
            try {
                final String responseJson = request.getResponse();
                rawCallback.accept(responseJson);
                LOG.debug("Received API response: {}", responseJson);
                response =
                        OBJECT_MAPPER.readValue(
                                request.getResponse(),
                                type);
            } catch (final Exception e) {
                throw new MinerException(e);
            }
        } else {
            throw new MinerException("Failed to obtain a response");
        }
        return response;
    }

    /**
     * Performs a rest query with basic auth.
     *
     * @param isGet             Whether or not the request is a GET.
     * @param host              The host.
     * @param port              The port.
     * @param path              The path.
     * @param username          The username.
     * @param password          The password.
     * @param content           The content.
     * @param responseProcessor The processor for responses.
     *
     * @throws IOException        on failure.
     * @throws URISyntaxException on failure.
     */
    private static void runRestQuery(
            final boolean isGet,
            final String host,
            final int port,
            final String path,
            final String username,
            final String password,
            final List<Map<String, Object>> content,
            final Consumer<String> responseProcessor,
            final int socketTimeout,
            final TimeUnit socketTimeoutUnits) throws IOException, URISyntaxException {
        final URI uri =
                new URI(
                        "http",
                        null,
                        host,
                        port,
                        path,
                        null,
                        null);
        final URL url = uri.toURL();

        final HttpHost targetHost =
                new HttpHost(
                        url.getHost(),
                        url.getPort(),
                        url.getProtocol());

        final CredentialsProvider provider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials(
                        username,
                        password);
        provider.setCredentials(
                AuthScope.ANY,
                credentials);

        final AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, new BasicScheme());

        final HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(provider);
        context.setAuthCache(authCache);

        final CloseableHttpClient httpClient =
                HttpClients
                        .custom()
                        .disableAutomaticRetries()
                        .setDefaultRequestConfig(
                                RequestConfig
                                        .custom()
                                        .setConnectTimeout((int) socketTimeoutUnits.toMillis(socketTimeout))
                                        .setSocketTimeout((int) socketTimeoutUnits.toMillis(socketTimeout))
                                        .build())
                        .build();

        final HttpRequest httpRequest;
        if (!isGet) {
            final HttpPost post = new HttpPost(url.getPath());
            final List<NameValuePair> params = new ArrayList<>();
            content.forEach(entry ->
                    params.add(
                            new BasicNameValuePair(
                                    entry.get("key").toString(),
                                    entry.get("value").toString())));
            post.setEntity(new UrlEncodedFormEntity(params));
            httpRequest = post;
        } else {
            httpRequest = new HttpGet(url.getPath());
        }

        try (final CloseableHttpResponse response =
                     httpClient.execute(
                             targetHost,
                             httpRequest,
                             context)) {
            final String responseBody =
                    EntityUtils.toString(response.getEntity());
            LOG.debug("Received API response: {}", responseBody);
            responseProcessor.accept(responseBody);
        }
    }
}