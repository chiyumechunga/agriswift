package zm.agriswift.entitlement.internal;

import zm.agriswift.entitlement.CropBuyoutRecorded;
import zm.agriswift.entitlement.Entitlement;
import zm.agriswift.entitlement.EntitlementRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validates a recorded delivery and, on success, publishes
 * {@link CropBuyoutRecorded} — this is the "Crop buyout recorded" box at the
 * top of disbursement_flow.png.
 */
@Service
class BuyoutService {

    private final EntitlementRepository entitlementRepository;
    private final ApplicationEventPublisher events;

    BuyoutService(EntitlementRepository entitlementRepository, ApplicationEventPublisher events) {
        this.entitlementRepository = entitlementRepository;
        this.events = events;
    }

    @Transactional
    public void recordAndValidate(Entitlement entitlement, boolean ziamisMatch) {
        entitlement.applyMoistureRuleAndPrice();
        entitlement.validate(ziamisMatch);
        entitlementRepository.save(entitlement);

        if (entitlement.getValidationStatus() == Entitlement.ValidationStatus.VALIDATED) {
            events.publishEvent(new CropBuyoutRecorded(
                    entitlement.getEntitlementId(),
                    entitlement.getFarmer().getFarmerId(),
                    entitlement.getPaymentAmount(),
                    "ZMW"));
        }
    }
}
