package zm.agriswift.farmer.internal.handlers;

import org.springframework.stereotype.Component;
import zm.agriswift.farmer.domain.Farmer;

import java.util.Map;

// Self‑registration
@Component
class SelfRegistrationHandler implements OnboardingHandler {
    @Override
    public Farmer.OnboardingChannel getChannel() { return Farmer.OnboardingChannel.SELF_REGISTRATION; }

    @Override
    public void apply(Farmer farmer, Map<String, Object> context) {
        farmer.assignSelfRegistration();
    }
}