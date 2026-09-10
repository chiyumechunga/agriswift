package zm.agriswift.identity.internal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import zm.agriswift.identity.internal.security.FarmerAuthenticationProvider;
import zm.agriswift.identity.internal.security.StaffUserDetailsService;

import java.util.List;

@Configuration
public class AuthenticationInfrastructureConfig {

    @Bean
    public AuthenticationManager authenticationManager(
            StaffUserDetailsService staffUserDetailsService,
            FarmerAuthenticationProvider farmerAuthenticationProvider,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider staffProvider = new DaoAuthenticationProvider(staffUserDetailsService);
        staffProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(List.of(staffProvider, farmerAuthenticationProvider));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }
}