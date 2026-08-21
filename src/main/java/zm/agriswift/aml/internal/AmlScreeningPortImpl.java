package zm.agriswift.aml.internal;

import org.springframework.stereotype.Component;
import zm.agriswift.farmer.api.AmlScreeningPort;

import java.util.UUID;

@Component
public class AmlScreeningPortImpl implements AmlScreeningPort {

    @Override
    public boolean isSanctioned(UUID farmerId) {
        // For POC, always return false (not sanctioned)
        return false;
    }
}