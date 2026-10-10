package zm.agriswift.common.exception;

import java.util.UUID;

public class TokenReuseDetectedException extends RuntimeException {
    public TokenReuseDetectedException(UUID userId) {
        super("Refresh token reuse detected for user " + userId + "; all sessions revoked.");
    }
}