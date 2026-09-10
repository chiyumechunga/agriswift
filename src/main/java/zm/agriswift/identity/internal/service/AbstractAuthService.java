package zm.agriswift.identity.internal.service;

import org.springframework.security.authentication.BadCredentialsException;
import zm.agriswift.identity.api.AccessTokenIssuer;
import zm.agriswift.identity.api.RefreshTokenRotator;
import zm.agriswift.identity.api.dto.IssuedTokens;
import zm.agriswift.identity.api.dto.PrincipalType;
import zm.agriswift.identity.api.dto.UserPrincipal;
import zm.agriswift.identity.domain.RefreshToken;
import zm.agriswift.identity.internal.repository.RefreshTokenRepository;
import zm.agriswift.identity.internal.security.RefreshTokenGenerator;

import java.time.Instant;
import java.util.UUID;

public abstract class AbstractAuthService implements AuthService, RefreshTokenRotator {

    protected final RefreshTokenRepository refreshTokenRepository;
    protected final AccessTokenIssuer tokenIssuer;   // was: AccessTokenProvider tokenProvider
    protected final long refreshTokenExpirationMs;

    protected AbstractAuthService(RefreshTokenRepository refreshTokenRepository,
                                  AccessTokenIssuer tokenIssuer,
                                  long refreshTokenExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    public  AuthResponse refresh(String presentedToken) {
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
        String newAccessToken = tokenIssuer.issue(principal).accessToken();   // narrow port
        String newRefreshToken = issueRefreshToken(principal);
        stored.revoke(RefreshTokenGenerator.hash(newRefreshToken), Instant.now());
        refreshTokenRepository.save(stored);
        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer", principal);
    }

    @Override
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }

    // --- RefreshTokenRotator port: delegation, no duplicated logic ---

    @Override
    public  IssuedTokens rotate(String presentedToken) {
        AuthResponse response = refresh(presentedToken);
        return new IssuedTokens(response.accessToken(), response.refreshToken(),
                refreshTokenExpirationMs / 1000);
    }

    @Override
    public void revoke(String presentedToken) {
        refreshTokenRepository.findByToken(RefreshTokenGenerator.hash(presentedToken))
                .ifPresent(stored -> {
                    stored.revoke(null, Instant.now());
                    refreshTokenRepository.save(stored);
                });
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