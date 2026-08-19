package zm.agriswift.farmer.api.dto;

import java.util.UUID;

public record PreferredPayoutAccount(
        UUID accountId,
        String accountTypeCode,
        short providerId
) {}