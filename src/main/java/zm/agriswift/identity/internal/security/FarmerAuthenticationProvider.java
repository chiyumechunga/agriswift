package zm.agriswift.identity.internal.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import zm.agriswift.identity.internal.service.AuthResponse;
import zm.agriswift.identity.internal.service.AuthService;
import zm.agriswift.identity.internal.service.LoginRequest;

import java.util.stream.Collectors;

@Component
public class FarmerAuthenticationProvider implements AuthenticationProvider {

    private final AuthService farmerAuthService;

    public FarmerAuthenticationProvider(@Qualifier("farmerAuthService") AuthService farmerAuthService) {
        this.farmerAuthService = farmerAuthService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        // Expect principal to be LoginRequest (set by our custom filter or controller)
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof LoginRequest request)) {
            return null;
        }
        // Delegate to farmerAuthService
        AuthResponse response = farmerAuthService.login(request);
        return new UsernamePasswordAuthenticationToken(
                response.user(),
                null,
                response.user().roles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet())
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}