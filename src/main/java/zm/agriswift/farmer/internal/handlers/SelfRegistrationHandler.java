package zm.agriswift.farmer.internal.handlers;

import org.springframework.stereotype.Component;
import zm.agriswift.farmer.domain.Farmer;
import zm.agriswift.farmer.domain.OnboardingChannel;

import java.util.Map;

// Self‑registration
@Component
class SelfRegistrationHandler implements OnboardingHandler {
    @Override
    public OnboardingChannel getChannel() { return OnboardingChannel.SELF_REGISTRATION; }

    @Override
    public void apply(Farmer farmer, Map<String, Object> context) {
        farmer.assignSelfRegistration();
    }
}