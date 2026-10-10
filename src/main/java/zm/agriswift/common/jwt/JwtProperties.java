package zm.agriswift.common.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "agriswift.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationMs,
        long refreshTokenExpirationMs
) {
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "agriswift.jwt.secret is not set. Provide AGRI_JWT_SECRET environment variable.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "agriswift.jwt.secret must be at least 32 bytes (256 bits) for HS256. "
                            + "Generate one with: openssl rand -base64 64");
        }
        if (accessTokenExpirationMs <= 0)  accessTokenExpirationMs  = 86_400_000L;  // 24h fallback
        if (refreshTokenExpirationMs <= 0) refreshTokenExpirationMs = 604_800_000L; // 7d fallback
    }
}