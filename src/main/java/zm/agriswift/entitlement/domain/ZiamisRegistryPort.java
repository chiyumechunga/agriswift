package zm.agriswift.entitlement;

import java.util.UUID;

/**
 * Port for integrating with the Ministry of Agriculture's ZIAMIS registry.
 * Used to verify that the farmer delivering the crop is a registered smallholder.
 */
public interface ZiamisRegistryPort {
    boolean isFarmerRegistered(UUID farmerId, String nationalIdHash);
}