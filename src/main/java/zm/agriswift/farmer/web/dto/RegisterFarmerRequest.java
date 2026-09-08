package zm.agriswift.farmer.web.dto;

import jakarta.validation.constraints.*;
import zm.agriswift.farmer.domain.OnboardingChannel;
import zm.agriswift.farmer.application.FarmerRegistrationCommand;

import java.time.LocalDate;
import java.util.UUID;

public record RegisterFarmerRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 80)
        String firstName,

        @Size(max = 80)
        String middleName,

        @NotBlank(message = "Last name is required")
        @Size(max = 80)
        String lastName,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @NotBlank(message = "National ID is required")
        String nationalId,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid mobile number format")
        String mobileNumber,

        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Preferred language is required")
        String preferredLanguage,

        @NotNull(message = "Onboarding channel is required")
        OnboardingChannel onboardingChannel,

        // Context for FRA officer registration (agentId and depotId)
        UUID registeringAgentId,

        int registeringDepotId

) {
    /**
     * Converts this web DTO into a domain command object.
     * The command contains all necessary data for the service layer.
     */
    public FarmerRegistrationCommand toCommand() {
        return new FarmerRegistrationCommand(
                firstName,
                middleName,
                lastName,
                dateOfBirth,
                nationalId,
                mobileNumber,
                email,
                preferredLanguage,
                onboardingChannel,
                registeringAgentId,
                registeringDepotId
        );
    }
}