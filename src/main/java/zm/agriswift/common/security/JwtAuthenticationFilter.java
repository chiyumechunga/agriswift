// common/security/JwtAuthenticationFilter.java
package zm.agriswift.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import zm.agriswift.common.jwt.JwtService;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtPrincipalLoader principalLoader;

    public JwtAuthenticationFilter(JwtService jwtService, JwtPrincipalLoader principalLoader) {
        this.jwtService = jwtService;
        this.principalLoader = principalLoader;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            bearer(request)
                    .flatMap(jwtService::parse)                                   // signature + expiry gate
                    .filter(c -> JwtService.TYPE_ACCESS.equals(
                            c.get(JwtService.CLAIM_TOKEN_TYPE, String.class)))    // refresh tokens rejected here
                    .flatMap(principalLoader::load)
                    .ifPresent(user -> {
                        var auth = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }
        filterChain.doFilter(request, response);
    }

    private Optional<String> bearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ")
                ? Optional.of(header.substring(7))
                : Optional.empty();
    }
}