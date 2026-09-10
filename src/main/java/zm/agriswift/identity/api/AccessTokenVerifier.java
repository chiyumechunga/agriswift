package zm.agriswift.identity.api;

import zm.agriswift.identity.api.dto.UserPrincipal;
import java.util.Optional;

public interface AccessTokenVerifier {
    Optional<UserPrincipal> verify(String rawToken);
}