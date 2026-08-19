package zm.agriswift.farmer.internal.handlers;

import zm.agriswift.farmer.domain.Farmer;

import java.util.Map;

// zm.agriswift.farmer.internal.handlers.OnboardingHandler
public interface OnboardingHandler {
    Farmer.OnboardingChannel getChannel();
    void apply(Farmer farmer, Map<String, Object> context);
}


