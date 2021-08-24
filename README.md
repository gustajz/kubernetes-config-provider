# Kubernetes Config Provider

### Development
```
./gradlew clean build shadowJar
```

### Usage

1. Copy uberjar to `/usr/share/java/kafka/`.

2. Configure provider in Kafka Connect.
```
config.providers=secrets
config.providers.secrets.class=com.github.gustajz.kafka.config.provider.KubernetesSecretConfigProvider
config.providers.secrets.param.namespace=default
```
> If you are using Docker or Kubernetes, you can set environment variables
>```
>CONNECT_CONFIG_PROVIDERS: secrets
>CONNECT_CONFIG_PROVIDERS_SECRETS_CLASS: com.github.gustajz.kafka.config.provider.KubernetesSecretConfigProvider
>CONNECT_CONFIG_PROVIDERS_SECRETS_PARAM_NAMESPACE: default
>```

3. Configure connector with secure properties.
```
username=${secrets:my-secret:my_username_key}
password=${secrets:my-secret:my_password_key}
```

## More Documentation

* [Externalizing Secrets](https://docs.confluent.io/platform/current/connect/security.html#externalizing-secrets)
* [KIP](https://cwiki.apache.org/confluence/display/KAFKA/KIP-297%3A+Externalizing+Secrets+for+Connect+Configurations)

## License

This project is licensed under the Apache License Version 2.0 (see
[LICENSE](./LICENSE)).
