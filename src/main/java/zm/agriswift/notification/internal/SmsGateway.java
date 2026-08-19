package zm.agriswift.notification.internal;

/** Placeholder for the real SMS aggregator integration (e.g. an MNO bulk-SMS API). */
public interface SmsGateway {
    void send(String maskedRecipientHint, String content);
}
