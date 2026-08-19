package zm.agriswift.audit;
import java.util.UUID;

/** Public API: any module records an action here; {@code userId} is null for system-initiated actions. */
public interface AuditRecorder {
    void record(UUID userId, String action, String resource, String resourceId);
}

