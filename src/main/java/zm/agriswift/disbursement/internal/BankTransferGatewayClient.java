package zm.agriswift.disbursement.internal;

import zm.agriswift.disbursement.Payment;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** DDACC rail via ZECHL/NFS (ZAMSwitch). Replace the TODO with the real pain.001 submission call. */
@Component
class BankTransferGatewayClient implements GatewayClient {

    @Override
    public boolean supports(Payment.ChannelType channelType) {
        return channelType == Payment.ChannelType.BANK_TRANSFER;
    }

    @Override
    public void submit(UUID paymentId, UUID uetr) {
        // TODO: build and send a pain.001 message to the NFS switch, persist
        // it via PaymentIsoMessage(OUTBOUND, "pain.001", ...) before returning.
    }
}

