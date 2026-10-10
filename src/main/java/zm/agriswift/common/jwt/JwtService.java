package zm.agriswift.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Single Responsibility: issuance and verification of JWTs only.
 * Session management, cookie handling and persistence live elsewhere.
 */
@Service
public class JwtService {

    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String TYPE_ACCESS  = "access";
    public static final String TYPE_REFRESH = "refresh";

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String ISSUER = "agriswift";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        // Keys.hmacShaKeyFor throws WeakKeyException if < 256 bits (belt & braces
        // on top of the JwtProperties validation).
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    // ─────────────────────────── ISSUANCE ───────────────────────────

    public String issueAccessToken(String subject, Map<String, Object> extraClaims) {
        return build(subject, TYPE_ACCESS, extraClaims, properties.accessTokenExpirationMs());
    }

    public String issueRefreshToken(String subject) {
        return build(subject, TYPE_REFRESH, Map.of(), properties.refreshTokenExpirationMs());
    }

    private String build(String subject, String tokenType, Map<String, Object> extraClaims, long ttlMs) {
        Instant now = Instant.now();

        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())          // jti: enables future revocation lists
                .issuer(ISSUER)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMs)));

        extraClaims.forEach(builder::claim);
        builder.claim(CLAIM_TOKEN_TYPE, tokenType);        // set LAST: callers cannot override it

        return builder.signWith(signingKey, Jwts.SIG.HS256).compact();
    }

    // ─────────────────────────── VERIFICATION ───────────────────────────

    /**
     * Parses and cryptographically verifies a token.
     * Expiry, signature, and malformed-structure checks are enforced by jjwt;
     * any failure yields an empty Optional rather than an exception.
     */
    public Optional<Claims> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(
                    Jwts.parser()
                            .verifyWith(signingKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            // ExpiredJwtException, UnsupportedJwtException, MalformedJwtException,
            // SignatureException all surface here. Never log the token itself.
            log.debug("JWT rejected: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> extractUsername(String token) {
        return parse(token).map(Claims::getSubject);
    }

    public Optional<String> extractTokenType(String token) {
        return parse(token).map(c -> c.get(CLAIM_TOKEN_TYPE, String.class));
    }

    /** True only if signature valid, unexpired, AND stamped as an access token. */
    public boolean isAccessTokenValid(String token, String expectedSubject) {
        return parse(token)
                .filter(c -> TYPE_ACCESS.equals(c.get(CLAIM_TOKEN_TYPE, String.class)))
                .filter(c -> expectedSubject == null || expectedSubject.equals(c.getSubject()))
                .isPresent();
    }

    /** Convenience overload matching the Spring Security filter contract. */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isAccessTokenValid(token, userDetails.getUsername());
    }

    // ─────────────────────────── HELPERS ───────────────────────────

    /** Useful for setting cookie Max-Age to exactly match the refresh token TTL. */
    public Duration refreshTokenTtl() {
        return Duration.ofMillis(properties.refreshTokenExpirationMs());
    }
}