package zm.agriswift.identity.internal.service;

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
import zm.agriswift.identity.domain.RefreshToken;
import zm.agriswift.identity.internal.repository.RefreshTokenRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class RefreshTokenService {

    public record TokenPair(String accessToken, String refreshToken, long expiresInMs) {}

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    /** Issues and persists a new refresh token for a freshly authenticated principal. */
    @Transactional
    public TokenPair issuePair(UUID userId, String username, PrincipalType principalType) {
        String refresh = persist(userId, username, principalType);
        String access  = jwtService.issueAccessToken(username, Map.of());
        return new TokenPair(access, refresh, jwtService.refreshTokenTtl().toMillis());
    }

    /**
     * Rotation: validates the presented token, revokes it, and issues a fresh pair.
     * Reuse of an already-rotated token triggers full revocation (replay-fraud response).
     */
    @Transactional
    public TokenPair rotate(String rawRefreshToken) {
        // 1. Cryptographic gate: signature, expiry, and typ=refresh
        String username = jwtService.extractUsername(rawRefreshToken)
                .filter(u -> jwtService.extractTokenType(rawRefreshToken)
                        .map(JwtService.TYPE_REFRESH::equals)
                        .orElse(false))
                .orElseThrow(InvalidRefreshTokenException::new);

        // 2. Server-side gate: existence and revocation state
        RefreshToken stored = repository.findByToken(rawRefreshToken)
                .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = Instant.now();

        if (stored.isRevoked()) {
            // A rotated token replayed = stolen token in the wild. Kill everything.
            log.warn("Refresh-token REUSE detected for user {}. Revoking all sessions.",
                    stored.getUserId());
            repository.revokeAllByUserId(stored.getUserId(), now);
            throw new TokenReuseDetectedException(stored.getUserId());
        }

        if (stored.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        // 3. Rotate: issue the replacement FIRST, then revoke the old token,
        //    linking it to its successor via the domain method (audit trail).
        TokenPair pair = issuePair(stored.getUserId(), username, stored.getPrincipalType());
        stored.revoke(pair.refreshToken(), now);
        repository.save(stored);
        return pair;
    }

    /** Logout / password-reset / admin kill-switch. */
    @Transactional
    public void revokeAll(UUID userId) {
        repository.revokeAllByUserId(userId, Instant.now());
    }

    private String persist(UUID userId, String username, PrincipalType principalType) {
        RefreshToken entity = new RefreshToken(
                jwtService.issueRefreshToken(username),
                userId,
                Instant.now().plus(jwtService.refreshTokenTtl()),
                principalType);                       // public 4-arg constructor
        return repository.save(entity).getToken();
    }
}