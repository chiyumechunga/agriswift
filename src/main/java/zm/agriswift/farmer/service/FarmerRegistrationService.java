package zm.agriswift.farmer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.common.exception.DomainException;
import zm.agriswift.farmer.domain.Farmer;
import zm.agriswift.farmer.domain.OnboardingChannel;
import zm.agriswift.farmer.internal.FarmerRepository;
import zm.agriswift.farmer.internal.PiiCipher;
import zm.agriswift.farmer.internal.handlers.OnboardingHandler;

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
    private final Map<OnboardingChannel, OnboardingHandler> onboardingHandlers;

    public FarmerRegistrationService(FarmerRepository farmerRepository,
                                     PiiCipher piiCipher,
                                     List<OnboardingHandler> handlers) {
        this.farmerRepository = farmerRepository;
        this.piiCipher = piiCipher;
        // Map handlers by the channel they support
        this.onboardingHandlers = handlers.stream()
                .collect(Collectors.toMap(OnboardingHandler::getChannel, Function.identity()));
    }

    public UUID registerFarmer(FarmerRegistrationCommand command) {
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
        // Use reflection or setter methods (add package‑private setters to Farmer if needed)
        farmer.setFarmerCode(generateFarmerCode()); // utility method
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
        OnboardingHandler handler = onboardingHandlers.get(command.channel());
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported onboarding channel: " + command.channel());
        }
        handler.apply(farmer, command.context()); // context contains agentId, depot, etc.

        // 5. Persist
        return farmerRepository.save(farmer).getFarmerId();
    }

    private String generateFarmerCode() {
        // e.g., "FRA" + timestamp + random suffix
        return "FRA" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);
    }
}