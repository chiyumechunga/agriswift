package zm.agriswift.identity.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "agriswift.security.authz")
public record AuthorizationProperties(List<String> publicEndpoints) {
    public AuthorizationProperties {
        publicEndpoints = publicEndpoints == null ? List.of() : List.copyOf(publicEndpoints);
    }
}