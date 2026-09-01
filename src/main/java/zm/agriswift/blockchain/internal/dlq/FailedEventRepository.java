package zm.agriswift.blockchain.internal.dlq;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FailedEventRepository extends JpaRepository<FailedEvent, UUID> {
    List<FailedEvent> findByRetryCountLessThan(int maxRetries);
}