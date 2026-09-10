package zm.agriswift.identity.api;

import zm.agriswift.identity.api.dto.IssuedTokens;
import zm.agriswift.identity.api.dto.UserPrincipal;

public interface AccessTokenIssuer {
    IssuedTokens issue(UserPrincipal principal);
}