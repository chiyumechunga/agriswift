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
import zm.agriswift.identity.internal.repository.FarmerCredentialRepository;
import zm.agriswift.identity.internal.repository.RefreshTokenRepository;
import zm.agriswift.identity.internal.security.AccessTokenProvider;

import java.util.Set;
import java.util.UUID;

@Service("farmerAuthService")
@Transactional
public class FarmerAuthService extends AbstractAuthService {

    private static final short MAX_PIN_ATTEMPTS = 3;
    private static final long LOCK_MINUTES = 30;

    private final FarmerCredentialRepository credentialRepository;
    private final PasswordEncoder pinEncoder;
    private final FarmerDirectory farmerDirectory;

    public FarmerAuthService(FarmerCredentialRepository credentialRepository,
                             PasswordEncoder pinEncoder,
                             AccessTokenProvider tokenProvider,
                             RefreshTokenRepository refreshTokenRepository,
                             FarmerDirectory farmerDirectory,
                             @Value("${agriswift.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        super(refreshTokenRepository, tokenProvider, refreshTokenExpirationMs);
        this.credentialRepository = credentialRepository;
        this.pinEncoder = pinEncoder;
        this.farmerDirectory = farmerDirectory;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier();
        if (identifier == null || identifier.isBlank()) {
            throw new BadCredentialsException("Identifier is required");
        }

        FarmerSummary farmer = farmerDirectory.findByMobileNumber(identifier)
                .or(() -> farmerDirectory.findByEmail(identifier))
                .or(() -> farmerDirectory.findByNationalId(identifier))
                .orElseThrow(() -> new BadCredentialsException("Invalid identifier"));

        // FIX 3: farmer_id is the business key now; PK is credential_id
        FarmerCredential credential = credentialRepository.findByFarmerId(farmer.farmerId())
                .orElseThrow(() -> new BadCredentialsException("No PIN set for this farmer"));

        if (credential.isLocked()) {
            throw new BadCredentialsException("Account locked");
        }

        // pin_hash is TEXT now; the entity exposes it as String — no charset bridge needed
        if (!pinEncoder.matches(request.pin(), credential.getPinHash())) {
            credential.recordFailedAttempt(MAX_PIN_ATTEMPTS, LOCK_MINUTES);
            credentialRepository.save(credential);
            throw new BadCredentialsException("Invalid PIN");
        }

        credential.resetFailedAttempts();
        credentialRepository.save(credential);

        UserPrincipal principal = new UserPrincipal(
                farmer.farmerId(),
                farmer.farmerId(),
                farmer.mobileNumber() != null ? farmer.mobileNumber() : identifier,
                farmer.email(),
                Set.of("FARMER"),
                null,
                true,
                PrincipalType.FARMER
        );

        String accessToken = tokenProvider.generateToken(principal);
        String refreshToken = issueRefreshToken(principal);

        return new AuthResponse(accessToken, refreshToken, "Bearer", principal);
    }


    @Override
    protected PrincipalType supportedType() { return PrincipalType.FARMER; }

    @Override
    protected UserPrincipal loadPrincipal(UUID userId) {
        FarmerSummary farmer = farmerDirectory.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Farmer not found"));

        return new UserPrincipal(
                farmer.farmerId(),
                farmer.farmerId(),
                farmer.mobileNumber(),
                farmer.email(),
                Set.of("FARMER"),
                null,
                true,
                PrincipalType.FARMER
        );
    }
}