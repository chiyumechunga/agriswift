package zm.agriswift.identity.internal;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import zm.agriswift.identity.domain.Role;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import zm.agriswift.identity.domain.User;
import zm.agriswift.identity.api.dto.UserPrincipal;
import zm.agriswift.identity.domain.RefreshToken;
import zm.agriswift.identity.internal.repository.UserRepository;
import zm.agriswift.identity.internal.repository.RefreshTokenRepository;
import zm.agriswift.identity.internal.security.JwtTokenProvider;
import zm.agriswift.identity.internal.security.RefreshTokenGenerator;
import zm.agriswift.identity.internal.security.SecurityUser;
import zm.agriswift.identity.internal.service.AuthResponse;
import zm.agriswift.identity.internal.service.AuthService;
import zm.agriswift.identity.internal.service.LoginRequest;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${agriswift.jwt.refresh-token-expiration-ms:604800000}") // 7 days, mirrors the access-token property naming
    private long refreshTokenExpirationMs;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtTokenProvider tokenProvider,
                           RefreshTokenRepository refreshTokenRepository,
                           UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
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
    @Transactional
    public AuthResponse refresh(String presentedToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(RefreshTokenGenerator.hash(presentedToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!stored.isActive(Instant.now())) {
            // A revoked or expired token being presented again means it was copied —
            // burn every session for this user, not just this one.
            refreshTokenRepository.revokeAllByUserId(stored.getUserId(), Instant.now());
            throw new BadCredentialsException("Refresh token reuse detected");
        }

        UserPrincipal principal = loadPrincipal(stored.getUserId());
        String newAccessToken = tokenProvider.generateToken(principal);
        String newRefreshToken = issueRefreshToken(principal.id());
        stored.revoke(RefreshTokenGenerator.hash(newRefreshToken), Instant.now());

        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer", principal);
    }

    @Override
    @Transactional
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
                .orElseThrow(() -> new BadCredentialsException("User no longer exists"));
        return new UserPrincipal(user.getId(), user.getFarmerId(), user.getUsername(), user.getEmail(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                user.getDepot() != null ? user.getDepot().getId() : null, user.isEnabled());
    }
}