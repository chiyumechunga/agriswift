package zm.agriswift.farmer.service;

import zm.agriswift.farmer.domain.OnboardingChannel;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Immutable command object carrying all data needed to register a new farmer.
 * Used by {@link FarmerRegistrationService}.
 */
public record FarmerRegistrationCommand(
        String firstName,
        String middleName,
        String lastName,
        LocalDate dateOfBirth,
        String nationalId,
        String mobileNumber,
        String email,
        String preferredLanguage,
        OnboardingChannel onboardingChannel,
        UUID registeringAgentId,
        int registeringDepotId
) {
    // Optional: validation can be added in the service if needed
}