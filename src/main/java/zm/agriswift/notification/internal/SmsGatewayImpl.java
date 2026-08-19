package zm.agriswift.notification.internal;

import org.springframework.stereotype.Component;

@Component
class SmsGatewayImpl implements SmsGateway {
    @Override
    public void send(String maskedRecipientHint, String content) {
        // TODO: call the real SMS aggregator.
    }
}

