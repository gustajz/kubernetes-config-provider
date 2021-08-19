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
    void testEmptyPath() {
        ConfigData configData = configProvider.get("", Collections.singleton("testKey"));
        assertTrue(configData.data().isEmpty());
        assertNull(configData.ttl());
    }

    @Test
    void testEmptyPathWithKey() {
        ConfigData configData = configProvider.get("");
        assertTrue(configData.data().isEmpty());
        assertNull(configData.ttl());
    }

    @Test
    void testNullPath() {
        ConfigData configData = configProvider.get(null);
        assertTrue(configData.data().isEmpty());
        assertNull(configData.ttl());
    }

    @Test
    void testNullPathWithKey() {
        ConfigData configData = configProvider.get(null, Collections.singleton("testKey"));
        assertTrue(configData.data().isEmpty());
        assertNull(configData.ttl());
    }
}
