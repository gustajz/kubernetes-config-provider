https://cwiki.apache.org/confluence/display/KAFKA/KIP-297%3A+Externalizing+Secrets+for+Connect+Configurations



```
./gradlew clean build shadowJar

```

```
plugin.path=/usr/share/connectors,/opt/secret-providers
```
or
```
# set the jar on the classpath
export CLASSPATH=kubernetes-config-provider-X.X.X-all.jar
```

```
config.providers=secrets
config.providers.secrets.class=com.github.gustajz.kafka.config.provider.KubernetesSecretConfigProvider
config.providers.secrets.param.namespace=default
```


```
# Properties specified in the Connector config
username=${secrets:my-secret:my_username_key}
password=${secrets:my-secret:my_password_key}
```
