package zm.agriswift.identity.internal.security;
import lombok.Getter;
import zm.agriswift.identity.api.dto.UserPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

public class SecurityUser implements UserDetails {

    @Getter
    private final UserPrincipal userPrincipal;
    private final String password;

    public SecurityUser(UserPrincipal userPrincipal, String password) {
        this.userPrincipal = userPrincipal;
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userPrincipal.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userPrincipal.username();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return userPrincipal.enabled(); }
}