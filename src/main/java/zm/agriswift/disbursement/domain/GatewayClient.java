package zm.agriswift.disbursement.domain;

import java.util.UUID;

/**
 * Abstraction over "the ZECHL NFS switch" box in disbursement_flow.png —
 * one implementation per rail (DDACC bank debit order, e-money/mobile-money).
 * Submission is fire-and-forget from this module's point of view: the real
 * outcome always arrives later via {@link zm.agriswift.disbursement.web.StatusCallbackController}.
 */
public interface GatewayClient {

    boolean supports(Payment payment); // Changed from ChannelType
    void submit(Payment payment);
}
