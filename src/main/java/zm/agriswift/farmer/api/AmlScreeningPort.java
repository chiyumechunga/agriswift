package zm.agriswift.farmer.api;

import java.util.UUID;

/**
 * Port (interface) for AML screening.
 * The implementation is provided by the AML module.
 * This is part of the Farmer module's public API.
 */
public interface AmlScreeningPort {
    boolean isSanctioned(UUID farmerId);
}