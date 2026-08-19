package zm.agriswift.identity.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Public API of the identity module — the only thing other modules
 * (disbursement, audit, entitlement, ...) are allowed to depend on when they
 * need to resolve "who is this actor" for an {@code actor_user_id} column.
 */
public interface UserDirectory {

    Optional<UserSummary> findById(UUID userId);

    record UserSummary(UUID userId, String userName, boolean active) {
    }
}

