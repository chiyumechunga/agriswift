package zm.agriswift.common.security;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class RequirePermissionAspect {

    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        // Extract all permissions/authorities from the logged-in user
        Set<String> userAuthorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String[] requiredPermissions = requirePermission.value();
        boolean authorized;

        if (requirePermission.operator() == RequirePermission.Logical.AND) {
            // User must possess EVERY permission listed
            authorized = Arrays.stream(requiredPermissions)
                    .allMatch(userAuthorities::contains);
        } else {
            // User must possess AT LEAST ONE permission listed
            authorized = Arrays.stream(requiredPermissions)
                    .anyMatch(userAuthorities::contains);
        }

        if (!authorized) {
            throw new AccessDeniedException("Insufficient permissions to execute: "
                    + joinPoint.getSignature().getName());
        }
    }
}