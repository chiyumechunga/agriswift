package zm.agriswift.disbursement.internal.bank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import zm.agriswift.disbursement.domain.GatewayClient;
import zm.agriswift.disbursement.domain.Payment;
import zm.agriswift.disbursement.domain.PaymentIsoMessage;
import zm.agriswift.disbursement.domain.PaymentIsoMessageRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class BankTransferGatewayClient implements GatewayClient {

    private static final Logger log = LoggerFactory.getLogger(BankTransferGatewayClient.class);
    private final PaymentIsoMessageRepository isoMessageRepository;

    @Value("${agriswift.zechl.enabled:false}")
    private boolean zechlEnabled;

    public BankTransferGatewayClient(PaymentIsoMessageRepository isoMessageRepository) {
        this.isoMessageRepository = isoMessageRepository;
    }

    @Override
    public boolean supports(Payment payment) {
        return payment.getChannelType() == Payment.ChannelType.BANK_TRANSFER;
    }

    @Override
    public void submit(Payment payment) {
        log.info("[ZECHL POE] Preparing pain.001.001.09 ISO 20022 message for paymentId: {}", payment.getPaymentId());

        String pain001Xml = String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.09">
                <CstmrCdtTrfInitn>
                    <GrpHdr>
                        <MsgId>%s</MsgId>
                        <CreDtTm>%s</CreDtTm>
                        <NbOfTxs>1</NbOfTxs>
                    </GrpHdr>
                    <PmtInf>
                        <PmtInfId>%s</PmtInfId>
                        <UETR>%s</UETR>
                        <InstdAmt Ccy="%s">%.2f</InstdAmt>
                    </PmtInf>
                </CstmrCdtTrfInitn>
            </Document>
            """,
                payment.getPaymentId(), LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                payment.getPaymentId(), payment.getUetr(), payment.getCurrency(), payment.getAmount()
        );

        isoMessageRepository.save(new PaymentIsoMessage(
                payment.getPaymentId(), PaymentIsoMessage.Direction.OUTBOUND, "pain.001.001.09", pain001Xml
        ));

        if (!zechlEnabled) {
            log.info("[ZECHL POE] ZECHL integration disabled. XML persisted to audit trail. Bypassing network transmission.");
            return;
        }

        // TODO: Transmit XML to ZECHL via SFTP or API when access is granted
        log.info("[ZECHL POE] Transmitting pain.001 message to ZECHL switch...");
    }
}