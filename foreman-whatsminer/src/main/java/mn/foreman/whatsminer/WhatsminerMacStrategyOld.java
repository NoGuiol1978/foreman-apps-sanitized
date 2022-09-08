package mn.foreman.whatsminer;

import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.MacStrategy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A strategy for obtaining MAC addresses from the old firmware. */
public class WhatsminerMacStrategyOld
        implements MacStrategy {

    /** The hosts pattern. */
    private static final Pattern HOSTS_PATTERN =
            Pattern.compile("var hosts = (\\{.*}})");

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(WhatsminerMacStrategyOld.class);

    /** The JSON mapper. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** The configuration. */
    private final ApplicationConfiguration applicationConfiguration;

    /** The IP. */
    private final String ip;

    /** The password. */
    private final String password;

    /** The port. */
    private final int port;

    /** The username. */
    private final String username;

    /**
     * Constructor.
     *
     * @param ip                       The IP.
     * @param port                     The port.
     * @param username                 The username.
     * @param password                 The password.
     * @param applicationConfiguration The configuration.
     */
    public WhatsminerMacStrategyOld(
            final String ip,
            final int port,
            final String username,
            final String password,
            final ApplicationConfiguration applicationConfiguration) {
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.password = password;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    public Optional<String> getMacAddress() {
        final AtomicReference<String> mac = new AtomicReference<>();
        try {
            WhatsminerQuery.query(
                    this.ip,
                    this.port,
                    this.username,
                    this.password,
                    Collections.singletonList(
                            WhatsminerQuery.Query
                                    .builder()
                                    .uri("/cgi-bin/luci/admin/status/overview")
                                    .isGet(true)
                                    .isMultipartForm(false)
                                    .urlParams(Collections.emptyList())
                                    .callback((statusCode, data) -> {
                                        if (statusCode == HttpStatus.SC_OK) {
                                            mac.set(
                                                    toMac(
                                                            this.ip,
                                                            data));
                                        }
                                    })
                                    .build()),
                    this.applicationConfiguration.getReadSocketTimeout());
        } catch (final Exception e) {
            LOG.warn("Failed to obtain MAC", e);
        }
        return Optional.ofNullable(mac.get());
    }

    /**
     * Obtains the MAC address from the HTML.
     *
     * @param ip   The IP.
     * @param data The HTML.
     *
     * @return The MAC.
     */
    private static String toMac(
            final String ip,
            final String data) {
        String macAddress = "";

        final Matcher matcher =
                HOSTS_PATTERN.matcher(
                        data.replace("\\", ""));
        if (matcher.find()) {
            try {
                final String hosts = matcher.group().replace("var hosts =", "");
                final Map<String, Map<String, String>> hostsMap =
                        OBJECT_MAPPER.readValue(
                                hosts,
                                new TypeReference<Map<String, Map<String, String>>>() {

                                });
                for (final Map.Entry<String, Map<String, String>> entry1 :
                        hostsMap.entrySet()) {
                    final Map<String, String> value = entry1.getValue();
                    if (value.getOrDefault("ipv4", "").equals(ip)) {
                        macAddress = entry1.getKey();
                    }
                }
            } catch (final Exception e) {
                // Ignore
            }
        }

        return macAddress;
    }
}
