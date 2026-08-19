package zm.agriswift.identity.api.dto;

import java.util.Set;
import java.util.UUID;

public record UserPrincipal(
        UUID id,                    // user_id for staff, farmer_id for farmers
        UUID farmerId,              // always present for farmers, nullable for staff
        String username,
        String email,
        Set<String> roles,
        UUID depotId,
        boolean enabled,
        PrincipalType principalType
) {}