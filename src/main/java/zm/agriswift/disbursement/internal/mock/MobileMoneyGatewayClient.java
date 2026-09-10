package zm.agriswift.disbursement.internal.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import zm.agriswift.disbursement.domain.GatewayClient;
import zm.agriswift.disbursement.domain.Payment;
import org.springframework.context.annotation.Profile;

import zm.agriswift.disbursement.internal.ProviderCodes;
import zm.agriswift.referencedata.PaymentProviderRepository;

@Component
@Profile({"dev", "poc", "default"}) // never active in prod
public class MobileMoneyGatewayClient implements GatewayClient {

    private static final Logger log = LoggerFactory.getLogger(MobileMoneyGatewayClient.class);

    private final PaymentProviderRepository providers;

    public MobileMoneyGatewayClient(PaymentProviderRepository providers) {
        this.providers = providers;
    }

    @Override
    public boolean supports(Payment payment) {
        return payment.getChannelType() == Payment.ChannelType.MOBILE_MONEY
                && !ProviderCodes.isAirtel(providers, payment);
    }

    @Override
    public void submit(Payment payment) {
        log.info("[MNO POE] Mocking API call to MNO switch for UETR: {}", payment.getUetr());
        // MockSwitchSimulator emits the async SUCCEEDED/FAILED callback shortly.
    }
}