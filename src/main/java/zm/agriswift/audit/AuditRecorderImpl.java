package zm.agriswift.audit;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class AuditRecorderImpl implements AuditRecorder {

    private final AuditLogRepository repository;

    AuditRecorderImpl(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(UUID userId, String action, String resource, String resourceId) {
        repository.save(AuditLog.of(userId, action, resource, resourceId));
    }
}

