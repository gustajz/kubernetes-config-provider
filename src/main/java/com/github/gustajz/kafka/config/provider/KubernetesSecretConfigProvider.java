package com.github.gustajz.kafka.config.provider;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.ClientBuilder;
import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.config.ConfigData;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.provider.ConfigProvider;

@Slf4j
public class KubernetesSecretConfigProvider implements ConfigProvider {

    private String namespace;

    /**
     * Retrieves the data at the given Secret.
     *
     * @param secretName the Secret where the data resides
     * @return the configuration data
     */
    @Override
    public ConfigData get(String secretName) {
        log.info(secretName);

        if (isBlank(secretName)) {
            return new ConfigData(Collections.emptyMap());
        }

        try {
            return new ConfigData(readSecretValues(secretName));
        } catch (IOException | ApiException ex) {
            throw new ConfigException(ex.getMessage(), ex);
        }
    }

    /**
     * Retrieves the data at the given Secret.
     *
     * @param secretName the Secret where the data resides
     * @param keys the keys whose values will be retrieved
     * @return the configuration data
     */
    @Override
    public ConfigData get(String secretName, Set<String> keys) {
        log.info(secretName);
        log.info("{}", keys);

        if (isBlank(secretName)) {
            return new ConfigData(Collections.emptyMap());
        }

        try {
            final Map<String, String> data = readSecretValues(secretName);
            final Map<String, String> filtered = new HashMap<>();

            keys.forEach(
                    key -> {
                        String value = data.getOrDefault(key, null);
                        if (value != null) {
                            filtered.put(key, value);
                        }
                    });

            return new ConfigData(filtered);

        } catch (IOException | ApiException ex) {
            throw new ConfigException(ex.getMessage(), ex);
        }
    }

    /**
     * Configure this class with the given key-value pairs.
     *
     * @param configs namespace=XXXX
     */
    @Override
    public void configure(Map<String, ?> configs) {
        this.namespace = (String) configs.getOrDefault("namespace", null);
        notNull(this.namespace);
    }

    @Override
    public void close() {}

    /**
     * Retrive data from Secret.
     *
     * @param secretName the Secret where the data resides
     * @return the configuration data
     * @throws IOException
     * @throws ApiException
     */
    private Map<String, String> readSecretValues(String secretName)
            throws IOException, ApiException {
        final Map<String, String> data = new HashMap<>();

        ApiClient client = ClientBuilder.defaultClient();
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();

        final V1Secret secret =
                api.readNamespacedSecret(secretName, this.namespace, null, null, null);

        ofNullable(secret.getData())
                .ifPresent(map -> map.forEach((s, bytes) -> data.put(s, new String(bytes))));

        ofNullable(secret.getStringData()).ifPresent(data::putAll);

        return data;
    }
}
