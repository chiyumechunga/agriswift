package zm.agriswift.blockchain.internal.firefly;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import zm.agriswift.blockchain.internal.FireFlyProperties;

@Configuration
@EnableConfigurationProperties(FireFlyProperties.class)
public class FireFlyConfig {

    /**
     * Self-contained FireFly REST client.
     * We build the WebClient directly instead of injecting the autoconfigured
     * WebClient.Builder, keeping this module independent of global autoconfiguration.
     */
    @Bean
    public WebClient fireflyWebClient(FireFlyProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.restUrl() + "/api/v1/namespaces/" + properties.namespace())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}