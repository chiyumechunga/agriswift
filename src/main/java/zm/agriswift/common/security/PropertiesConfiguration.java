package zm.agriswift.common.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import zm.agriswift.identity.internal.config.AuthorizationProperties;

@Configuration
@EnableConfigurationProperties({WebSecurityProperties.class, AuthorizationProperties.class})
public class PropertiesConfiguration {
}