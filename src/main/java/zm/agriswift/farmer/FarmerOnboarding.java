package zm.agriswift.farmer;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Exposed onboarding API of the Farmer module.
 * Other modules (e.g. Identity) must depend ONLY on this type,
 * never on farmer.application / farmer.domain internals.
 */
public interface FarmerOnboarding {

    UUID registerSelfRegisteredFarmer(SelfRegistrationRequest request);

    record SelfRegistrationRequest(
            String nationalId,
            String mobileNumber,
            String firstName,
            String middleName,
            String lastName,
            LocalDate dateOfBirth,
            String email,
            String preferredLanguage
    ) {}
}