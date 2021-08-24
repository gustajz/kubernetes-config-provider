package com.github.gustajz.kafka.config.provider;

import static org.junit.jupiter.api.Assertions.*;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.common.config.ConfigData;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@EnableKubernetesMockClient(crud = true)
@SetEnvironmentVariable(
        key = "KUBECONFIG",
        value = KubernetesSecretConfigProviderTest.TMP_CONFIG_FILE)
class KubernetesSecretConfigProviderTest {

    static final String TMP_CONFIG_FILE = "config.yaml";

    static KubernetesClient client;

    KubernetesSecretConfigProvider configProvider;

    @BeforeAll
    static void init() throws IOException {
        Config configuration = client.getConfiguration();

        String templateFile =
                KubernetesSecretConfigProviderTest.class.getResource("/config-tpl.yaml").getFile();

        String template =
                FileUtils.readFileToString(
                        Paths.get(templateFile).toFile(), StandardCharsets.UTF_8);

        FileUtils.writeStringToFile(
                Paths.get(TMP_CONFIG_FILE).toFile(),
                template.replace("<server>", configuration.getMasterUrl()),
                StandardCharsets.UTF_8);

        Map<String, String> result = new HashMap<>();
        result.put("testKey", "testResult");
        result.put("testKey2", "testResult2");

        client.secrets()
                .inNamespace("my-ns")
                .create(
                        new SecretBuilder()
                                .withType("Opaque")
                                .withMetadata(new ObjectMetaBuilder().withName("my-secret").build())
                                .withStringData(result)
                                .build());
    }

    @AfterAll
    static void cleanUp() {
        FileUtils.deleteQuietly(Paths.get(TMP_CONFIG_FILE).toFile());
    }

    @BeforeEach
    public void setup() {
        configProvider = new KubernetesSecretConfigProvider();
        Map<String, String> configs = new HashMap<>();
        configs.put("namespace", "my-ns");

        configProvider.configure(configs);
    }

    @Test
    void testGetAllKeys() {
        ConfigData configData = configProvider.get("my-secret");
        Map<String, String> result = new HashMap<>();
        result.put("testKey", "testResult");
        result.put("testKey2", "testResult2");
        assertEquals(result, configData.data());
        assertNull(configData.ttl());
    }

    @Test
    void testGetOneKey() {
        ConfigData configData = configProvider.get("my-secret", Collections.singleton("testKey"));
        Map<String, String> result = new HashMap<>();
        result.put("testKey", "testResult");
        assertEquals(result, configData.data());
        assertNull(configData.ttl());
    }

    @Test
    void testKeyNotFound() {
        ConfigException thrown =
                assertThrows(
                        ConfigException.class,
                        () ->
                                configProvider.get(
                                        "my-secret", Collections.singleton("testKeyNotFound")));
        assertTrue(thrown.getMessage().contains("my-secret"));
        assertTrue(thrown.getMessage().contains("testKeyNotFound"));
    }

    @Test
    void testEmptyPath() {
        ConfigException thrown =
                assertThrows(
                        ConfigException.class,
                        () -> configProvider.get("", Collections.singleton("testKey")));
        assertTrue(
                thrown.getMessage()
                        .contains(
                                "secretName cannot be null or empty. Review your configuration."));
    }

    @Test
    void testEmptyPathWithKey() {
        ConfigException thrown = assertThrows(ConfigException.class, () -> configProvider.get(""));
        assertTrue(
                thrown.getMessage()
                        .contains(
                                "secretName cannot be null or empty. Review your configuration."));
    }

    @Test
    void testNullPath() {
        ConfigException thrown =
                assertThrows(ConfigException.class, () -> configProvider.get(null));
        assertTrue(
                thrown.getMessage()
                        .contains(
                                "secretName cannot be null or empty. Review your configuration."));
    }

    @Test
    void testNoSecretFoundException() {
        ConfigException thrown =
                assertThrows(
                        ConfigException.class,
                        () -> configProvider.get("notExists", Collections.singleton("testKey")));
        assertTrue(thrown.getMessage().contains("notExists"));
    }

    @Test
    void testNullPathWithKey() {
        ConfigException thrown =
                assertThrows(
                        ConfigException.class,
                        () -> configProvider.get(null, Collections.singleton("testKey")));
        assertTrue(
                thrown.getMessage()
                        .contains(
                                "secretName cannot be null or empty. Review your configuration."));
    }
}
