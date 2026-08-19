package zm.agriswift.disbursement.internal;

import zm.agriswift.disbursement.StatusCallbackRequest;
import org.springframework.stereotype.Component;

/**
 * Placeholder HMAC/mTLS signature check for inbound gateway callbacks.
 * Replace with the real ZECHL-issued shared secret / certificate validation
 * before this endpoint is exposed outside a trusted network.
 */
@Component
class CallbackSignatureVerifier {

    boolean isValid(StatusCallbackRequest request) {
        return request.signature() != null && !request.signature().isBlank();
    }
}

