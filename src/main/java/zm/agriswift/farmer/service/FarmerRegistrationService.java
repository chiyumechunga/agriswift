package zm.agriswift.farmer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.common.exception.DomainException;
import zm.agriswift.farmer.domain.Farmer;
import zm.agriswift.farmer.domain.OnboardingChannel;
import zm.agriswift.farmer.internal.FarmerRepository;
import zm.agriswift.farmer.internal.PiiCipher;
import zm.agriswift.farmer.internal.handlers.OnboardingHandler;
import zm.agriswift.referencedata.Depot;
import zm.agriswift.referencedata.DepotRepository;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class FarmerRegistrationService {

    private final FarmerRepository farmerRepository;
    private final PiiCipher piiCipher;
    private final DepotRepository depotRepository;
    private final Map<OnboardingChannel, OnboardingHandler> onboardingHandlers;

    public FarmerRegistrationService(FarmerRepository farmerRepository,
                                     PiiCipher piiCipher,
                                     DepotRepository depotRepository,
                                     List<OnboardingHandler> handlers) {
        this.farmerRepository = farmerRepository;
        this.piiCipher = piiCipher;
        this.depotRepository = depotRepository;
        this.onboardingHandlers = handlers.stream()
                .collect(Collectors.toMap(OnboardingHandler::getChannel, Function.identity()));
    }

    public UUID registerFarmer(FarmerRegistrationCommand command) {
        try {
            // 1. Check uniqueness using blind indexes
            byte[] nationalIdHash = piiCipher.generateBlindIndex(command.nationalId());
            byte[] mobileHash = command.mobileNumber() != null
                    ? piiCipher.generateBlindIndex(command.mobileNumber())
                    : null;

            if (farmerRepository.findByNationalIdHash(nationalIdHash).isPresent()) {
                throw new DomainException("National ID already registered.");
            }
            if (mobileHash != null && farmerRepository.findByMobileNumberHash(mobileHash).isPresent()) {
                throw new DomainException("Mobile number already registered.");
            }

            // 2. Encrypt PII
            byte[] nationalIdCipher = piiCipher.encrypt(command.nationalId());
            byte[] mobileCipher = command.mobileNumber() != null
                    ? piiCipher.encrypt(command.mobileNumber())
                    : null;

            // 3. Build the Farmer entity
            Farmer farmer = new Farmer();
            farmer.setFarmerCode(generateFarmerCode());
            farmer.setFirstName(command.firstName());
            farmer.setMiddleName(command.middleName());
            farmer.setLastName(command.lastName());
            farmer.setDateOfBirth(command.dateOfBirth());
            farmer.setNationalIdCiphertext(nationalIdCipher);
            farmer.setNationalIdHash(nationalIdHash);
            farmer.setMobileNumberCiphertext(mobileCipher);
            farmer.setMobileNumberHash(mobileHash);
            farmer.setEmail(command.email());
            farmer.setPreferredLanguage(command.preferredLanguage());

            // 4. Apply onboarding strategy (Open/Closed principle)
            OnboardingHandler handler = onboardingHandlers.get(command.onboardingChannel());
            if (handler == null) {
                throw new IllegalArgumentException("Unsupported onboarding channel: " + command.onboardingChannel());
            }

            // Build context map for the handler
            Map<String, Object> context = Map.of(
                    "agentId", command.registeringAgentId(),
                    "depot", fetchDepot(command.registeringDepotId())
            );
            handler.apply(farmer, context);

            // 5. Persist
            return farmerRepository.save(farmer).getFarmerId();
        } catch (GeneralSecurityException e) {
            throw new DomainException("Failed to process PII data: " + e.getMessage(), e);
        }
    }

    private Depot fetchDepot(Integer depotId) {
        if (depotId == null) {
            return null;
        }
        return depotRepository.findById(depotId)
                .orElseThrow(() -> new DomainException("Depot not found with id: " + depotId));
    }

    private String generateFarmerCode() {
        return "FRA" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);
    }
}