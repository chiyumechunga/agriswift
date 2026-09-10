package zm.agriswift.identity.api;

import zm.agriswift.identity.api.dto.IssuedTokens;

public interface RefreshTokenRotator {
    IssuedTokens rotate(String refreshToken);
    void revoke(String refreshToken);
}