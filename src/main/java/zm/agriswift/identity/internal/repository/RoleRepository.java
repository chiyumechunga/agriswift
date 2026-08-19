package zm.agriswift.identity.internal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zm.agriswift.identity.domain.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Short> {
    Optional<Role> findByRoleName(String roleName);
}

