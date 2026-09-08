package zm.agriswift.farmer.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.common.exception.DomainException;
import zm.agriswift.farmer.domain.Farmer;
import zm.agriswift.farmer.domain.OnboardingChannel;
import zm.agriswift.farmer.internal.FarmerRepository;
import zm.agriswift.farmer.internal.PiiCipher;
import zm.agriswift.referencedata.Depot;
import zm.agriswift.referencedata.DepotRepository;

import java.security.GeneralSecurityException;
import java.util.UUID;

@Service
@Transactional
public class FarmerRegistrationService {

    private final FarmerRepository farmerRepository;
    private final PiiCipher piiCipher;
    private final DepotRepository depotRepository;

    public FarmerRegistrationService(FarmerRepository farmerRepository,
                                     PiiCipher piiCipher,
                                     DepotRepository depotRepository) {
        this.farmerRepository = farmerRepository;
        this.piiCipher = piiCipher;
        this.depotRepository = depotRepository;
    }

    public UUID registerFarmer(FarmerRegistrationCommand command) {
        try {
            // 1. Uniqueness checks using blind indexes
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

            // 3. Fetch depot if required
            Depot depot = null;
            if (command.onboardingChannel() != OnboardingChannel.SELF_REGISTRATION) {
                depot = depotRepository.findById(command.registeringDepotId())
                        .orElseThrow(() -> new DomainException("Depot not found: " + command.registeringDepotId()));
            }

            // 4. Build Farmer using the factory method (encapsulation)
            Farmer farmer = Farmer.register(
                    generateFarmerCode(),
                    command.firstName(),
                    command.middleName(),
                    command.lastName(),
                    command.dateOfBirth(),
                    nationalIdCipher,
                    nationalIdHash,
                    mobileCipher,
                    mobileHash,
                    command.email(),
                    command.preferredLanguage(),
                    command.onboardingChannel(),
                    command.registeringAgentId(),
                    depot
            );

            // 5. Persist
            return farmerRepository.save(farmer).getFarmerId();

        } catch (GeneralSecurityException e) {
            throw new DomainException("Failed to process PII data: " + e.getMessage(), e);
        }
    }

    private String generateFarmerCode() {
        return "FRA" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);
    }
}