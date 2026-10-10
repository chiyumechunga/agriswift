package zm.agriswift.farmer.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.common.exception.DomainException;
import zm.agriswift.common.exception.NotFoundException;
import zm.agriswift.farmer.api.AmlScreeningPort;
import zm.agriswift.farmer.domain.Farmer;
import zm.agriswift.farmer.domain.KycDocument;
import zm.agriswift.farmer.internal.FarmerRepository;
import zm.agriswift.farmer.internal.KycDocumentRepository;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class KycService {

    private static final Logger log = LoggerFactory.getLogger(KycService.class);

    private final FarmerRepository farmerRepository;
    private final KycDocumentRepository documentRepository;
    private final AmlScreeningPort amlScreeningPort;

    public KycService(FarmerRepository farmerRepository,
                      KycDocumentRepository documentRepository,
                      AmlScreeningPort amlScreeningPort) {
        this.farmerRepository = farmerRepository;
        this.documentRepository = documentRepository;
        this.amlScreeningPort = amlScreeningPort;
    }

    public void verifyFarmer(UUID farmerId, UUID verifiedByAgentId) {
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new NotFoundException("Farmer not found: " + farmerId));

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
        if (amlScreeningPort.isSanctioned(farmer.getFarmerId())) {
            farmer.markKycRejected();
            farmerRepository.save(farmer);
            throw new DomainException("AML screening failed.");
        }

        // 3. All checks passed – mark as verified
        farmer.markKycVerified(Instant.now());
        farmerRepository.save(farmer);

        // The verifying agent is part of the compliance trail.
        // V2: route through the audit module's AuditRecorder once the farmer
        // module is granted an "audit" dependency in its package-info.
        log.info("KYC verified for farmer {} by agent {}.", farmerId, verifiedByAgentId);
    }

    public void rejectKyc(UUID farmerId, String reason) {
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new NotFoundException("Farmer not found: " + farmerId));
        farmer.markKycRejected();
        farmerRepository.save(farmer);

        // V2: persist the rejection reason on the Farmer aggregate;
        // for now it must at least reach the log, never silently drop.
        log.warn("KYC rejected for farmer {}: {}", farmerId, reason);
    }
}