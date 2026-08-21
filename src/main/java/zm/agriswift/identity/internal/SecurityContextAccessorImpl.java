package zm.agriswift.identity.internal;
import zm.agriswift.common.exception.UnauthorizedException;
import zm.agriswift.identity.api.SecurityContextAccessor;
import zm.agriswift.identity.api.dto.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import zm.agriswift.identity.internal.security.SecurityUser;

import java.util.Optional;

@Component
public class SecurityContextAccessorImpl implements SecurityContextAccessor {

    @Override
    public Optional<UserPrincipal> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }

        if (auth.getPrincipal() instanceof SecurityUser securityUser) {
            return Optional.of(securityUser.getUserPrincipal());
        }

        return Optional.empty();
    }

    @Override
    public UserPrincipal getRequiredCurrentUser() {
        return getCurrentUser().orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
    }
}
