/**
 * Audit module: append-only action log (SEC-18: UPDATE/DELETE revoked at the
 * DB grant level, see the Flyway migration). Exposes a synchronous
 * {@link AuditRecorder} API — other modules call this directly rather than
 * via an event, since an audit-write should happen in the same transaction
 * as the action it's recording.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common"}
)
package zm.agriswift.audit;