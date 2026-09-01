package zm.agriswift.entitlement.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zm.agriswift.entitlement.CropBuyoutRecorded;
import zm.agriswift.entitlement.Entitlement;
import zm.agriswift.entitlement.EntitlementRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.entitlement.ZiamisRegistryPort;

/**
 * Validates a recorded delivery and, on success, publishes
 * {@link CropBuyoutRecorded} — this is the "Crop buyout recorded" box at the
 * top of disbursement_flow.png.
 */

@Service
public class BuyoutService {

    private static final Logger log = LoggerFactory.getLogger(BuyoutService.class);
    private final EntitlementRepository entitlementRepository;
    private final ApplicationEventPublisher events;
    private final ZiamisRegistryPort ziamisRegistryPort;

    public BuyoutService(EntitlementRepository entitlementRepository,
                         ApplicationEventPublisher events,
                         ZiamisRegistryPort ziamisRegistryPort) {
        this.entitlementRepository = entitlementRepository;
        this.events = events;
        this.ziamisRegistryPort = ziamisRegistryPort;
    }

    @Transactional
    public void recordAndValidate(Entitlement entitlement, String nationalIdHash) {
        entitlement.applyMoistureRuleAndPrice();
        entitlementRepository.save(entitlement);

        boolean ziamisMatch = ziamisRegistryPort.isFarmerRegistered(
                entitlement.getFarmerId(), nationalIdHash);

        entitlement.validate(ziamisMatch);
        entitlementRepository.save(entitlement);

        if (entitlement.getValidationStatus() == Entitlement.ValidationStatus.VALIDATED) {
            events.publishEvent(new CropBuyoutRecorded(
                    entitlement.getEntitlementId(),
                    entitlement.getFarmerId(),
                    entitlement.getPaymentAmount(),
                    "ZMW"));
        }
    }
}