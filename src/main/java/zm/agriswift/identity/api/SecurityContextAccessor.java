package zm.agriswift.identity.api;

import zm.agriswift.identity.api.dto.UserPrincipal;

import java.util.Optional;

public interface SecurityContextAccessor {

    Optional<UserPrincipal> getCurrentUser();

    UserPrincipal getRequiredCurrentUser();
}