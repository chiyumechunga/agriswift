package zm.agriswift.identity.internal.service;

import org.springframework.security.authentication.BadCredentialsException;
import zm.agriswift.identity.api.dto.PrincipalType;
import zm.agriswift.identity.api.dto.UserPrincipal;
import zm.agriswift.identity.domain.RefreshToken;
import zm.agriswift.identity.internal.repository.RefreshTokenRepository;
import zm.agriswift.identity.internal.security.AccessTokenProvider;
import zm.agriswift.identity.internal.security.RefreshTokenGenerator;

import java.time.Instant;
import java.util.UUID;

public abstract class AbstractAuthService implements AuthService {

    protected final RefreshTokenRepository refreshTokenRepository;
    protected final AccessTokenProvider tokenProvider;
    protected final long refreshTokenExpirationMs;

    protected AbstractAuthService(RefreshTokenRepository refreshTokenRepository,
                                  AccessTokenProvider tokenProvider,
                                  long refreshTokenExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    public final AuthResponse refresh(String presentedToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(RefreshTokenGenerator.hash(presentedToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (stored.getPrincipalType() != supportedType()) {
            throw new BadCredentialsException("Invalid refresh token for this principal type");
        }
        if (!stored.isActive(Instant.now())) {
            refreshTokenRepository.revokeAllByUserId(stored.getUserId(), Instant.now());
            throw new BadCredentialsException("Refresh token reuse detected");
        }
        UserPrincipal principal = loadPrincipal(stored.getUserId());
        String newAccessToken = tokenProvider.generateToken(principal);
        String newRefreshToken = issueRefreshToken(principal);
        stored.revoke(RefreshTokenGenerator.hash(newRefreshToken), Instant.now());
        refreshTokenRepository.save(stored);
        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer", principal);
    }

    @Override
    public final void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }

    protected String issueRefreshToken(UserPrincipal principal) {
        String raw = RefreshTokenGenerator.generateRaw();
        refreshTokenRepository.save(new RefreshToken(
                RefreshTokenGenerator.hash(raw), principal.id(),
                Instant.now().plusMillis(refreshTokenExpirationMs),
                principal.principalType()));
        return raw;
    }

    protected abstract PrincipalType supportedType();
    protected abstract UserPrincipal loadPrincipal(UUID userId);
}