package zm.agriswift.entitlement.internal;

import zm.agriswift.entitlement.Entitlement;
import zm.agriswift.entitlement.EntitlementLookup;
import zm.agriswift.entitlement.EntitlementRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class EntitlementLookupImpl implements EntitlementLookup {

    private final EntitlementRepository entitlementRepository;

    EntitlementLookupImpl(EntitlementRepository entitlementRepository) {
        this.entitlementRepository = entitlementRepository;
    }

    @Override
    public Optional<EntitlementView> findById(UUID entitlementId) {
        return entitlementRepository.findById(entitlementId)
                .map(e -> new EntitlementView(
                        e.getEntitlementId(),
                        e.getFarmer().getFarmerId(),
                        e.getPaymentAmount(),
                        e.getValidationStatus().name()));
    }
}
