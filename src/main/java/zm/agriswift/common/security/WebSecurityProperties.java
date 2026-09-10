package zm.agriswift.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "agriswift.security.web")
public record WebSecurityProperties(List<String> allowedOrigins, String contentSecurityPolicy) {
    public WebSecurityProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}