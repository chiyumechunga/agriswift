package zm.agriswift.disbursement.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import zm.agriswift.disbursement.domain.GatewayClient;
import zm.agriswift.disbursement.domain.Payment;
import java.util.UUID;

@Component
public class MobileMoneyGatewayClient implements GatewayClient {
    private static final Logger log = LoggerFactory.getLogger(MobileMoneyGatewayClient.class);

    @Override
    public boolean supports(Payment.ChannelType channelType) {
        return channelType == Payment.ChannelType.MOBILE_MONEY;
    }

    @Override
    public void submit(UUID paymentId, UUID uetr) {
        log.info("[MNO POE] Mocking API call to MTN/Airtel for UETR: {}", uetr);
        // TODO: Real implementation would POST JSON to MNO gateway with mTLS
    }
}