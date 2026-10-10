package zm.agriswift.identity.internal.integration;

import java.time.LocalDate;

public record InrisProfile(
        String firstName,
        String middleName,
        String lastName,
        LocalDate dateOfBirth,
        String province,
        String district
) {}