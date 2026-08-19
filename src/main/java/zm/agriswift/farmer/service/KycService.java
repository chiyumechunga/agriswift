package zm.agriswift.farmer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.common.exception.DomainException;
import zm.agriswift.common.exception.NotFoundException;
import zm.agriswift.farmer.domain.Farmer;
import zm.agriswift.farmer.domain.KycDocument;
import zm.agriswift.farmer.internal.FarmerRepository;
import zm.agriswift.farmer.internal.KycDocumentRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class KycService {

    private final FarmerRepository farmerRepository;
    private final KycDocumentRepository documentRepository;
    private final AmlScreeningPort amlScreeningPort; // Port (interface) for external AML

    public KycService(FarmerRepository farmerRepository,
                      KycDocumentRepository documentRepository,
                      AmlScreeningPort amlScreeningPort) {
        this.farmerRepository = farmerRepository;
        this.documentRepository = documentRepository;
        this.amlScreeningPort = amlScreeningPort;
    }

    public void verifyFarmer(UUID farmerId, UUID verifiedByAgentId) {
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new NotFoundException("Farmer not found"));

        // 1. Validate that all mandatory KYC documents are uploaded
        boolean allDocsPresent = documentRepository.findByFarmer_FarmerId(farmerId)
                .stream()
                .map(KycDocument::getDocumentType)
                .collect(Collectors.toSet())
                .containsAll(Set.of(KycDocument.DocumentType.NRC_SCAN,
                        KycDocument.DocumentType.FACIAL_PHOTO));

        if (!allDocsPresent) {
            throw new DomainException("Missing mandatory KYC documents.");
        }

        // 2. AML screening via a port (dependency inversion)
        if (amlScreeningPort.isSanctioned(farmer)) {
            farmer.markKycRejected();
            farmerRepository.save(farmer);
            throw new DomainException("AML screening failed.");
        }

        // 3. All checks passed – mark as verified
        farmer.markKycVerified(Instant.now());
        farmerRepository.save(farmer);
    }

    public void rejectKyc(UUID farmerId, String reason) {
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new NotFoundException("Farmer not found"));
        farmer.markKycRejected();
        farmerRepository.save(farmer);
        // Optionally persist the rejection reason in a separate log.
    }
}