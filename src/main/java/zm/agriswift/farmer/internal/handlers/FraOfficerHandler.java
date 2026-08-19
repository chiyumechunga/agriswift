package zm.agriswift.farmer.internal.handlers;


import org.springframework.stereotype.Component;
import zm.agriswift.farmer.domain.Farmer;
import zm.agriswift.referencedata.Depot;

import java.util.Map;
import java.util.UUID;

// Example implementation for FRA officer
@Component
class FraOfficerHandler implements OnboardingHandler {
    @Override
    public Farmer.OnboardingChannel getChannel() { return Farmer.OnboardingChannel.FRA_DEPOT; }

    @Override
    public void apply(Farmer farmer, Map<String, Object> context) {
        UUID agentId = (UUID) context.get("agentId");
        Depot depot = (Depot) context.get("depot");
        farmer.assignFraOfficerRegistration(agentId, depot, Farmer.OnboardingChannel.FRA_DEPOT);
    }
}