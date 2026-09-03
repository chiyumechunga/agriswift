package zm.agriswift.disbursement.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import zm.agriswift.disbursement.domain.GatewayClient;
import zm.agriswift.disbursement.domain.Payment;
import zm.agriswift.disbursement.domain.PaymentIsoMessage;
import zm.agriswift.disbursement.PaymentIsoMessageRepository;

import java.util.UUID;

@Component
public class BankTransferGatewayClient implements GatewayClient {

    private static final Logger log = LoggerFactory.getLogger(BankTransferGatewayClient.class);
    private final PaymentIsoMessageRepository isoMessageRepository;

    public BankTransferGatewayClient(PaymentIsoMessageRepository isoMessageRepository) {
        this.isoMessageRepository = isoMessageRepository;
    }

    @Override
    public boolean supports(Payment.ChannelType channelType) {
        return channelType == Payment.ChannelType.BANK_TRANSFER;
    }

    @Override
    public void submit(UUID paymentId, UUID uetr) {
        log.info("[ZECHL POE] Preparing pain.001 ISO 20022 message for paymentId: {}", paymentId);

        String mockPain001 = String.format("""
            { "Document": { "CstmrCdtTrfInitn": { "GrpHdr": { "MsgId": "%s", "NbOfTxs": "1" }, "PmtInf": { "UETR": "%s" } } } }
            """, paymentId.toString(), uetr.toString());

        isoMessageRepository.save(new PaymentIsoMessage(
                paymentId, PaymentIsoMessage.Direction.OUTBOUND, "pain.001", mockPain001
        ));
        log.info("[ZECHL POE] pain.001 message persisted to audit trail.");
    }
}