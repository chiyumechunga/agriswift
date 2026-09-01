package zm.agriswift.blockchain.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agriswift.firefly")
public record FireFlyProperties(
        String restUrl,
        String webSocketUrl,
        String namespace,
        String subscriptionName
) {}