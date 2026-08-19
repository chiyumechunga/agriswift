package zm.agriswift.farmer.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record KycVerificationRequest(

        @NotNull(message = "Verifying agent ID is required")
        UUID verifiedByAgentId,

        // Optional reason for rejection (used for REJECTED status)
        String reason

) {}