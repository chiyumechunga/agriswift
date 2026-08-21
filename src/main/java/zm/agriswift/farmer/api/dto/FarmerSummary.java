package zm.agriswift.farmer.api.dto;

import org.springframework.modulith.NamedInterface;

import java.util.UUID;

@NamedInterface
public record FarmerSummary(
        UUID farmerId,
        String farmerCode,
        String fullName,
        String mobileNumber,   // decrypted (or plain if we choose to store plain)
        String email,
        boolean kycVerified,
        boolean isActive
) {}