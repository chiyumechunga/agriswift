// identity/internal/security/SecurityUserLoader.java — rebuilds SecurityUser from claims
package zm.agriswift.identity.internal.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import zm.agriswift.common.security.JwtPrincipalLoader;
import zm.agriswift.identity.api.dto.PrincipalType;
import zm.agriswift.identity.api.dto.UserPrincipal;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SecurityUserLoader implements JwtPrincipalLoader {

    @Override
    public Optional<UserDetails> load(Claims claims) {
        try {
            UUID id = UUID.fromString(claims.getSubject());
            String farmerIdStr = claims.get("farmerId", String.class);
            Object rolesClaim = claims.get("roles");
            Set<String> roles = rolesClaim instanceof List<?> l
                    ? l.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet())
                    : Set.of();
            String typeStr = claims.get("principalType", String.class);
            PrincipalType type = typeStr != null ? PrincipalType.valueOf(typeStr) : PrincipalType.STAFF;

            UserPrincipal principal = new UserPrincipal(
                    id,
                    farmerIdStr != null ? UUID.fromString(farmerIdStr) : null,
                    claims.get("username", String.class),
                    claims.get("email", String.class),
                    roles,
                    claims.get("depotId", Integer.class),
                    true,
                    type);
            return Optional.of(new SecurityUser(principal, "")); // password irrelevant post-authentication
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}