package zm.agriswift.identity.internal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.farmer.api.FarmerDirectory;
import zm.agriswift.farmer.api.dto.FarmerSummary;
import zm.agriswift.identity.api.dto.PrincipalType;
import zm.agriswift.identity.api.dto.UserPrincipal;
import zm.agriswift.identity.domain.FarmerCredential;
import zm.agriswift.identity.domain.RefreshToken;
import zm.agriswift.identity.internal.repository.FarmerCredentialRepository;
import zm.agriswift.identity.internal.repository.RefreshTokenRepository;
import zm.agriswift.identity.internal.security.AccessTokenProvider;
import zm.agriswift.identity.internal.security.RefreshTokenGenerator;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service("farmerAuthService")
@Transactional
public class FarmerAuthService implements AuthService {

    private final FarmerCredentialRepository credentialRepository;
    private final PasswordEncoder pinEncoder;
    private final AccessTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FarmerDirectory farmerDirectory;
    private final long refreshTokenExpirationMs;

    public FarmerAuthService(FarmerCredentialRepository credentialRepository,
                             PasswordEncoder pinEncoder,
                             AccessTokenProvider tokenProvider,
                             RefreshTokenRepository refreshTokenRepository,
                             FarmerDirectory farmerDirectory,
                             @Value("${agriswift.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.credentialRepository = credentialRepository;
        this.pinEncoder = pinEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.farmerDirectory = farmerDirectory;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier();
        if (identifier == null || identifier.isBlank()) {
            throw new BadCredentialsException("Identifier is required");
        }

        // Try to find farmer by mobile, email, or NRC (in that order)
        FarmerSummary farmer = farmerDirectory.findByMobileNumber(identifier)
                .or(() -> farmerDirectory.findByEmail(identifier))
                .or(() -> farmerDirectory.findByNationalId(identifier))
                .orElseThrow(() -> new BadCredentialsException("Invalid identifier"));

        // Load credential
        FarmerCredential credential = credentialRepository.findById(farmer.farmerId())
                .orElseThrow(() -> new BadCredentialsException("No PIN set for this farmer"));

        // Check lockout
        if (credential.isLocked()) {
            throw new BadCredentialsException("Account locked");
        }

        // Verify PIN
        if (!pinEncoder.matches(request.pin(), credential.getPinHash())) {
            credential.recordFailedAttempt((short) 3, 30);
            credentialRepository.save(credential);
            throw new BadCredentialsException("Invalid PIN");
        }

        // Reset attempts on success
        credential.resetFailedAttempts();
        credentialRepository.save(credential);

        // Build UserPrincipal for farmer
        UserPrincipal principal = new UserPrincipal(
                farmer.farmerId(),
                farmer.farmerId(),
                farmer.mobileNumber() != null ? farmer.mobileNumber() : identifier, // fallback
                farmer.email(),
                Set.of("FARMER"),
                null,
                true,
                PrincipalType.FARMER
        );

        // Issue tokens
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
        FarmerSummary farmer = farmerDirectory.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Farmer not found"));

        return new UserPrincipal(
                farmer.farmerId(),          // UUID
                farmer.farmerId(),          // UUID (same as id for farmers)
                farmer.mobileNumber(),      // String (mobile as username)
                farmer.email(),             // String (may be null)
                Set.of("FARMER"),           // roles
                null,                       // depotId is always null for farmers
                true,                       // enabled
                PrincipalType.FARMER
        );
    }
}