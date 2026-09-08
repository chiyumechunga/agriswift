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

    @Bean
    public WebClient fireflyWebClient(FireFlyProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.getRestUrl() + "/api/v1/namespaces/" + properties.getNamespace())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}