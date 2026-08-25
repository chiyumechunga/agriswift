package zm.agriswift.identity.internal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.identity.api.dto.PrincipalType;
import zm.agriswift.identity.api.dto.UserPrincipal;
import zm.agriswift.identity.domain.RefreshToken;
import zm.agriswift.identity.domain.Role;
import zm.agriswift.identity.domain.User;
import zm.agriswift.identity.internal.repository.RefreshTokenRepository;
import zm.agriswift.identity.internal.repository.UserRepository;
import zm.agriswift.identity.internal.security.AccessTokenProvider;
import zm.agriswift.identity.internal.security.RefreshTokenGenerator;
import zm.agriswift.identity.internal.security.SecurityUser;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Service("staffAuthService")
@Transactional
public class StaffAuthService implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AccessTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final long refreshTokenExpirationMs;

    public StaffAuthService(
            AuthenticationManager authenticationManager,
            AccessTokenProvider tokenProvider,
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            @Value("${agriswift.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        // Pattern-matched cast: removes the NPE warning and fails fast on unexpected principals
        if (!(authentication.getPrincipal() instanceof SecurityUser securityUser)) {
            throw new BadCredentialsException("Staff authentication did not produce a SecurityUser");
        }
        UserPrincipal principal = securityUser.getUserPrincipal();

        String accessToken = tokenProvider.generateToken(principal);
        String refreshToken = issueRefreshToken(principal);
        return new AuthResponse(accessToken, refreshToken, "Bearer", principal);
    }

    @Override
    public AuthResponse refresh(String presentedToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(RefreshTokenGenerator.hash(presentedToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        // Hardening: a farmer token must never be redeemable on the staff path
        if (stored.getPrincipalType() != PrincipalType.STAFF) {
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
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }

    private String issueRefreshToken(UserPrincipal principal) {
        String raw = RefreshTokenGenerator.generateRaw();
        refreshTokenRepository.save(new RefreshToken(
                RefreshTokenGenerator.hash(raw),
                principal.id(),
                Instant.now().plusMillis(refreshTokenExpirationMs),
                principal.principalType()));   // 4th arg: schema's principal_type discriminator
        return raw;
    }

    private UserPrincipal loadPrincipal(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        return new UserPrincipal(
                user.getUserId(),
                user.getFarmerId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream()
                        .map(Role::getRoleName)
                        .collect(Collectors.toSet()),
                user.getDepot() != null ? user.getDepot().getDepotId() : null,
                user.isActive(),
                PrincipalType.STAFF);
    }
}