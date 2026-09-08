package zm.agriswift.disbursement.application;

import zm.agriswift.disbursement.domain.GatewayClient;
import zm.agriswift.disbursement.domain.Payment;
import org.springframework.stereotype.Component;

import java.util.List;

/** "Payout gateway router" box: picks bank transfer vs. mobile money and hands off to the matching client. */
@Component
class PayoutGatewayRouter {

    private final List<GatewayClient> clients;

    PayoutGatewayRouter(List<GatewayClient> clients) {
        this.clients = clients;
    }

    void route(Payment payment) {
        GatewayClient client = clients.stream()
                .filter(c -> c.supports(payment.getChannelType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No gateway client registered for channel " + payment.getChannelType()));
        client.submit(payment.getPaymentId(), payment.getUetr());
        payment.markExecuted();
    }
}
