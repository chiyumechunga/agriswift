package zm.agriswift.entitlement.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import zm.agriswift.entitlement.ZiamisRegistryPort;

import java.util.UUID;

@Component
public class MockZiamisRegistryPort implements ZiamisRegistryPort {

    private static final Logger log = LoggerFactory.getLogger(MockZiamisRegistryPort.class);

    @Override
    public boolean isFarmerRegistered(UUID farmerId, String nationalIdHash) {
        log.info("[POC MOCK] Simulating ZIAMIS registry check for farmerId: {}. Returning TRUE.", farmerId);
        // TODO: Replace with real REST/SOAP call to ZIAMIS in production
        return true;
    }
}