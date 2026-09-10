package zm.agriswift.disbursement.internal.airtel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import zm.agriswift.disbursement.domain.GatewayClient;
import zm.agriswift.disbursement.domain.Payment;
import zm.agriswift.disbursement.internal.ProviderCodes;
import zm.agriswift.referencedata.PaymentProviderRepository;

@Component
public class AirtelMoneyGatewayClient implements GatewayClient {

    private static final Logger log = LoggerFactory.getLogger(AirtelMoneyGatewayClient.class);

    private final AirtelProperties properties;
    private final PaymentProviderRepository providers;

    public AirtelMoneyGatewayClient(AirtelProperties properties, PaymentProviderRepository providers) {
        this.properties = properties;
        this.providers = providers;
    }

    @Override
    public boolean supports(Payment payment) {
        return payment.getChannelType() == Payment.ChannelType.MOBILE_MONEY
                && ProviderCodes.isAirtel(providers, payment);
    }

    @Override
    public void submit(Payment payment) {
        log.info("[AIRTEL V3 POE] Preparing B2P disbursement for UETR: {}", payment.getUetr());

        if (!properties.enabled()) {
            log.warn("[AIRTEL V3 POE] Sandbox not provisioned (agriswift.disbursement.airtel.enabled=false). "
                            + "Dry-run UETR {}: amount={} {}, recipientAccount={}",
                    payment.getUetr(), payment.getAmount(), payment.getCurrency(), payment.getPaymentAccountId());
            return; // MockSwitchSimulator completes the async lifecycle in dev/poc
        }

        // Transport PR fills this in; until then refuse to fake a live disbursement.
        throw new IllegalStateException(
                "Airtel v3 transport not wired yet - refusing to fake live disbursement for UETR "
                        + payment.getUetr());
    }
}