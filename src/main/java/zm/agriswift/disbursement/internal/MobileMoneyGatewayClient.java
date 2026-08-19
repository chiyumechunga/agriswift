package zm.agriswift.disbursement.internal;

import org.springframework.stereotype.Component;
import zm.agriswift.disbursement.Payment;

import java.util.UUID;

/** Mobile-money rail (MTN/Airtel/Zamtel), routed through the same NFS switch. */
@Component
class MobileMoneyGatewayClient implements GatewayClient {

    @Override
    public boolean supports(Payment.ChannelType channelType) {
        return channelType == Payment.ChannelType.MOBILE_MONEY;
    }

    @Override
    public void submit(UUID paymentId, UUID uetr) {
        // TODO: call the MNO disbursement API / e-money rail.
    }
}