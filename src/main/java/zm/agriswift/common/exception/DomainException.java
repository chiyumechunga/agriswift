package zm.agriswift.common.exception;

/** Base type for module-level business-rule violations (not framework/plumbing errors). */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

