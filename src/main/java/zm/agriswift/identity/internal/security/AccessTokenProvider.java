package zm.agriswift.identity.internal.security;

import zm.agriswift.identity.api.dto.UserPrincipal;

public interface AccessTokenProvider {
    String generateToken(UserPrincipal principal);
    boolean validateToken(String token);
    UserPrincipal getUserPrincipalFromToken(String token);
}