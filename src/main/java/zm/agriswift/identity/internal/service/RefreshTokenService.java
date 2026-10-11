package zm.agriswift.identity.internal.service;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Shared kernel imports
import zm.agriswift.common.exception.InvalidRefreshTokenException;
import zm.agriswift.common.exception.TokenReuseDetectedException;
import zm.agriswift.common.jwt.JwtService;

// Identity module imports
import zm.agriswift.identity.api.dto.PrincipalType;
import zm.agriswift.identity.api.dto.UserPrincipal;
import zm.agriswift.identity.domain.RefreshToken;
import zm.agriswift.identity.internal.repository.RefreshTokenRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RefreshTokenService {

    /** What a client receives on login / rotation. */
    public record TokenPair(String accessToken, String refreshToken, long expiresInMs) {}

    /** Rotation result: the new pair plus enough info to rebuild the AuthResponse. */
    public record RotatedSession(TokenPair tokens, UUID userId, PrincipalType principalType) {}

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    /** Issues and persists a fresh access+refresh pair for a newly authenticated principal. */
    @Transactional
    public TokenPair issuePair(UserPrincipal principal) {
        Map<String, Object> claims = claimsFor(principal);
        String refresh = persist(principal, claims);
        String access  = jwtService.issueAccessToken(principal.id().toString(), claims);
        // expiresInMs describes the ACCESS token — that is what the client must renew by.
        return new TokenPair(access, refresh, jwtService.accessTokenTtl().toMillis());
    }

    /**
     * Rotation: validates the presented token, revokes it, and issues a fresh pair.
     * Reuse of an already-rotated token triggers full revocation (replay-fraud response).
     */
    @Transactional
    public RotatedSession rotate(String rawRefreshToken) {
        // 1. Cryptographic gate: signature + expiry (jjwt), then typ=refresh.
        //    A refresh token is self-describing: it carries the claims needed to
        //    re-issue the access token without a database principal lookup.
        Claims claims = jwtService.parse(rawRefreshToken)
                .filter(c -> JwtService.TYPE_REFRESH.equals(
                        c.get(JwtService.CLAIM_TOKEN_TYPE, String.class)))
                .orElseThrow(InvalidRefreshTokenException::new);

        // 2. Server-side gate: existence and revocation state.
        RefreshToken stored = repository.findByToken(rawRefreshToken)
                .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = Instant.now();

        if (stored.isRevoked()) {
            // A rotated token replayed = stolen token in the wild. Kill every session.
            log.warn("Refresh-token REUSE detected for user {}. Revoking all sessions.",
                    stored.getUserId());
            repository.revokeAllByUserId(stored.getUserId(), now);
            throw new TokenReuseDetectedException(stored.getUserId());
        }

        if (stored.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        // 3. Issue the successor FIRST, then revoke the old token, linking it to
        //    its successor via the domain method (audit trail).
        TokenPair pair = issuePair(principalFrom(claims));
        stored.revoke(pair.refreshToken(), now);
        repository.save(stored);

        return new RotatedSession(pair, stored.getUserId(), stored.getPrincipalType());
    }

    /** Logout / password-reset / admin kill-switch. */
    @Transactional
    public void revokeAll(UUID userId) {
        repository.revokeAllByUserId(userId, Instant.now());
    }

    // ─────────────────────────── internals ───────────────────────────

    /** Claim set shared by access and refresh tokens; mirrors JwtTokenProvider's shape. */
    private Map<String, Object> claimsFor(UserPrincipal p) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", p.username());
        claims.put("email", p.email());
        claims.put("roles", p.roles());
        claims.put("depotId", p.depotId());
        claims.put("farmerId", p.farmerId() != null ? p.farmerId().toString() : null);
        claims.put("principalType", p.principalType().name());
        return claims;
    }

    /** Rebuilds the principal from a verified refresh token's claims. */
    private UserPrincipal principalFrom(Claims claims) {
        UUID id = UUID.fromString(claims.getSubject());
        String farmerIdStr = claims.get("farmerId", String.class);
        Object rolesClaim = claims.get("roles");
        Set<String> roles = rolesClaim instanceof List<?> list
                ? list.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet())
                : Set.of();
        String typeStr = claims.get("principalType", String.class);
        PrincipalType type = typeStr != null ? PrincipalType.valueOf(typeStr) : PrincipalType.STAFF;

        return new UserPrincipal(
                id,
                farmerIdStr != null ? UUID.fromString(farmerIdStr) : null,
                claims.get("username", String.class),
                claims.get("email", String.class),
                roles,
                claims.get("depotId", Integer.class),
                true,
                type);
    }

    private String persist(UserPrincipal principal, Map<String, Object> claims) {
        RefreshToken entity = new RefreshToken(
                jwtService.issueRefreshToken(principal.username(), claims),
                principal.id(),
                Instant.now().plus(jwtService.refreshTokenTtl()),
                principal.principalType());              // public 4-arg constructor
        return repository.save(entity).getToken();
    }
}