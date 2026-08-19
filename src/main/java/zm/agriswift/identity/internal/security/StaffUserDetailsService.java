package zm.agriswift.identity.internal.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.identity.api.dto.PrincipalType;
import zm.agriswift.identity.api.dto.UserPrincipal;
import zm.agriswift.identity.domain.Role;
import zm.agriswift.identity.domain.User;
import zm.agriswift.identity.internal.repository.UserRepository;

import java.util.stream.Collectors;

/**
 * Loads staff (system) users from the database during authentication.
 * Used exclusively by Spring Security's DaoAuthenticationProvider.
 *
 * <p>This service is package‑private to enforce module encapsulation; it is
 * only accessible to the Identity module's internal security configuration.
 */
@Service
public class StaffUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    StaffUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Convert domain User to a UserPrincipal DTO
        UserPrincipal principal = new UserPrincipal(
                user.getUserId(),
                user.getFarmerId(),                     // may be null for pure staff
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream()
                        .map(Role::getRoleName)
                        .collect(Collectors.toSet()),
                user.getDepot() != null ? user.getDepot().getDepotId() : null,
                user.isActive(),
                PrincipalType.STAFF
        );

        // Wrap in SecurityUser (implements UserDetails)
        return new SecurityUser(principal, user.getPasswordHash());
    }
}