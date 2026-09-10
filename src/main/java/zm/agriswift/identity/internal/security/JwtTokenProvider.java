package zm.agriswift.identity.internal.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import zm.agriswift.identity.api.AccessTokenIssuer;
import zm.agriswift.identity.api.AccessTokenVerifier;
import zm.agriswift.identity.api.dto.IssuedTokens;
import zm.agriswift.identity.api.dto.PrincipalType;
import zm.agriswift.identity.api.dto.UserPrincipal;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider implements AccessTokenIssuer, AccessTokenVerifier {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${agriswift.jwt.secret}") String secret,
            @Value("${agriswift.jwt.access-token-expiration-ms:86400000}") long accessTokenExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    @Override
    public IssuedTokens issue(UserPrincipal principal) {
        String token = generateToken(principal);
        // Access tokens don't contain the refresh token. The Auth Service will merge them later.
        return new IssuedTokens(token, null, accessTokenExpirationMs / 1000);
    }

    @Override
    public Optional<UserPrincipal> verify(String rawToken) {
        if (!validateToken(rawToken)) {
            return Optional.empty();
        }
        try {
            return Optional.of(getUserPrincipalFromToken(rawToken));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // --- Private Implementation Details ---

    private String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(principal.id().toString())
                .claim("farmerId", principal.farmerId() != null ? principal.farmerId().toString() : null)
                .claim("username", principal.username())
                .claim("email", principal.email())
                .claim("roles", principal.roles())
                .claim("depotId", principal.depotId())
                .claim("principalType", principal.principalType().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    private boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private UserPrincipal getUserPrincipalFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID id = UUID.fromString(claims.getSubject());
        String farmerIdStr = claims.get("farmerId", String.class);
        UUID farmerId = farmerIdStr != null ? UUID.fromString(farmerIdStr) : null;
        String username = claims.get("username", String.class);
        String email = claims.get("email", String.class);

        Object rolesClaim = claims.get("roles");
        Set<String> roles = rolesClaim instanceof List<?> list
                ? list.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet())
                : Set.of();

        Integer depotId = claims.get("depotId", Integer.class);
        String typeStr = claims.get("principalType", String.class);
        PrincipalType principalType = typeStr != null ? PrincipalType.valueOf(typeStr) : PrincipalType.STAFF;

        return new UserPrincipal(id, farmerId, username, email, roles, depotId, true, principalType);
    }
}