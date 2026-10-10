package zm.agriswift.common.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import zm.agriswift.identity.internal.security.DelegatedAccessDeniedHandler;
import zm.agriswift.identity.internal.security.DelegatedAuthenticationEntryPoint;

import java.util.List;

@Configuration
public class ApiSecurityPolicy {

    private final TransportSecurityPolicy transport;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final DelegatedAuthenticationEntryPoint authenticationEntryPoint;
    private final DelegatedAccessDeniedHandler accessDeniedHandler;



    public ApiSecurityPolicy(
            TransportSecurityPolicy transport,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Qualifier("delegatedAuthenticationEntryPoint") AuthenticationEntryPoint authenticationEntryPoint,
            @Qualifier("delegatedAccessDeniedHandler") AccessDeniedHandler accessDeniedHandler) {
        this.transport = transport;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = (DelegatedAuthenticationEntryPoint) authenticationEntryPoint;
        this.accessDeniedHandler = (DelegatedAccessDeniedHandler) accessDeniedHandler;
    }

    @Bean @Order(1)
    SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
        transport.customize(http);
        http.securityMatcher(
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/farmer/login",
                        "/api/v1/auth/farmer/refresh")      // duplicates removed
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    @Bean @Order(5)
    SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        transport.customize(http);
        http.securityMatcher("/api/v1/external/**", "/api/v1/webhooks/**")
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    @Bean @Order(10)
    SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
        transport.customize(http);
        http.securityMatcher("/api/v1/**")
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    /** Deny-by-default for everything the chains above don't claim (see finding 2). */
    @Bean @Order(Ordered.LOWEST_PRECEDENCE)
    SecurityFilterChain fallbackChain(HttpSecurity http) throws Exception {
        transport.customize(http);
        http.authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health/**").permitAll()
                .anyRequest().denyAll());
        return http.build();
    }

    /** Keep the JWT filter out of the global servlet filter chain (see finding 3). */
    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter f) {
        var reg = new FilterRegistrationBean<>(f);
        reg.setEnabled(false);
        return reg;
    }
}