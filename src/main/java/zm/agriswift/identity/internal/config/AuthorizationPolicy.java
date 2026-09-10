package zm.agriswift.identity.internal.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableConfigurationProperties(AuthorizationProperties.class)
@Order(30)
public class AuthorizationPolicy implements Customizer<HttpSecurity> {

    private final AuthorizationProperties properties;

    public AuthorizationPolicy(AuthorizationProperties properties) {
        this.properties = properties;
    }

    @Override
    public void customize(HttpSecurity http) {
        String[] publicEndpoints = properties.publicEndpoints().toArray(new String[0]);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(publicEndpoints).permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated());
    }
}