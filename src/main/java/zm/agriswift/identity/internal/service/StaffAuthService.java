package zm.agriswift.identity.internal.service;

import org.springframework.beans.factory.annotation.Qualifier;
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
class StaffAuthService implements AuthService {

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
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        UserPrincipal principal = securityUser.getUserPrincipal();

        String accessToken = tokenProvider.generateToken(principal);
        String refreshToken = issueRefreshToken(principal.id());
        return new AuthResponse(accessToken, refreshToken, "Bearer", principal);
    }

    @Override
    public AuthResponse refresh(String presentedToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(RefreshTokenGenerator.hash(presentedToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!stored.isActive(Instant.now())) {
            refreshTokenRepository.revokeAllByUserId(stored.getUserId(), Instant.now());
            throw new BadCredentialsException("Refresh token reuse detected");
        }

        UserPrincipal principal = loadPrincipal(stored.getUserId());
        String newAccessToken = tokenProvider.generateToken(principal);
        String newRefreshToken = issueRefreshToken(principal.id());
        stored.revoke(RefreshTokenGenerator.hash(newRefreshToken), Instant.now());
        refreshTokenRepository.save(stored);

        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer", principal);
    }

    @Override
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }

    private String issueRefreshToken(UUID userId) {
        String raw = RefreshTokenGenerator.generateRaw();
        refreshTokenRepository.save(new RefreshToken(
                RefreshTokenGenerator.hash(raw), userId, Instant.now().plusMillis(refreshTokenExpirationMs)));
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
                user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet()),
                user.getDepot() != null ? user.getDepot().getDepotId() : null,
                user.isActive(),
                PrincipalType.STAFF
        );
    }
}