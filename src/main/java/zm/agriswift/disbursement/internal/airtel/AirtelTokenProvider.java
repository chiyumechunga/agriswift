package zm.agriswift.disbursement.internal.airtel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Component
@EnableConfigurationProperties(AirtelProperties.class)
@ConditionalOnProperty(name = "agriswift.disbursement.airtel.enabled", havingValue = "true")
public class AirtelTokenProvider {

    private final AirtelProperties properties;
    private final WebClient webClient;

    private record TokenResponse(String access_token, Long expires_in) {}
    private record CachedToken(String token, Instant expiresAt) {}
    private volatile CachedToken cached;

    public AirtelTokenProvider(AirtelProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder().baseUrl(properties.baseUrl()).build();
    }

    public String getAccessToken() {
        CachedToken current = cached;
        if (current != null && Instant.now().isBefore(current.expiresAt())) {
            return current.token();
        }
        synchronized (this) {
            current = cached;
            if (current != null && Instant.now().isBefore(current.expiresAt())) {
                return current.token();
            }

            String basicAuth = Base64.getEncoder().encodeToString(
                    (properties.clientId() + ":" + properties.clientSecret())
                            .getBytes(StandardCharsets.UTF_8));

            TokenResponse response = webClient.post()
                    .uri("/auth/oauth2/token?grant_type=client_credentials")
                    .header("Authorization", "Basic " + basicAuth)
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block(Duration.ofSeconds(15));

            if (response == null || response.access_token() == null) {
                throw new IllegalStateException("Airtel OAuth2 token request failed");
            }

            long ttl = response.expires_in() != null ? response.expires_in() : 3600;
            cached = new CachedToken(response.access_token(),
                    Instant.now().plusSeconds(ttl).minusSeconds(60)); // 60s safety buffer
            log.info("Airtel OAuth2 token acquired (TTL {}s)", ttl);
            return cached.token();
        }
    }
}