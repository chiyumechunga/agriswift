package zm.agriswift.farmer.api.dto;

import java.util.UUID;

public record FarmerSummary(
        UUID farmerId,
        String farmerCode,
        String fullName,
        String mobileNumber,   // decrypted (or plain if we choose to store plain)
        String email,
        boolean kycVerified,
        boolean isActive
) {}