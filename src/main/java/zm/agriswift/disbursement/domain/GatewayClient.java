package zm.agriswift.disbursement.domain;

import java.util.UUID;

/**
 * Abstraction over "the ZECHL NFS switch" box in disbursement_flow.png —
 * one implementation per rail (DDACC bank debit order, e-money/mobile-money).
 * Submission is fire-and-forget from this module's point of view: the real
 * outcome always arrives later via {@link com.agriswift.disbursement.internal.StatusCallbackController}.
 */
public interface GatewayClient {

    boolean supports(Payment.ChannelType channelType);

    /** Submits the payment instruction and returns immediately; does not block for settlement. */
    void submit(UUID paymentId, UUID uetr);
}
