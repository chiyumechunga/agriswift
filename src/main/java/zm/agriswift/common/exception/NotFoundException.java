package zm.agriswift.common.exception;

/**
 * Raised when an aggregate cannot be located by its identifier.
 * The missing identifier belongs in the message, not in a second
 * constructor parameter — keep the contract single-argument so every
 * module throws it uniformly.
 */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }
}