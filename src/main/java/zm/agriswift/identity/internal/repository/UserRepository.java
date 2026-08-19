package zm.agriswift.identity.internal.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import zm.agriswift.identity.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByExternalSubject(String externalSubject);

    Optional<User> findByUsername(String username);
}
