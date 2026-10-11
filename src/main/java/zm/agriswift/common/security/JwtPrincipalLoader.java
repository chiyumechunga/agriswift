// common/security/JwtPrincipalLoader.java (port; identity supplies the bean)
package zm.agriswift.common.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Optional;

public interface JwtPrincipalLoader {
    Optional<UserDetails> load(Claims claims);
}