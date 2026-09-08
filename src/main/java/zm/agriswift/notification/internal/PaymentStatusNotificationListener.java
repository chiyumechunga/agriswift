package zm.agriswift.notification.internal;
import zm.agriswift.disbursement.api.PaymentStatusChanged;
import zm.agriswift.notification.Notification;
import zm.agriswift.notification.NotificationRepository;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** "Farmer notified" box: sends an SMS confirming payout status once it's known. */
@Component
class PaymentStatusNotificationListener {

    private final NotificationRepository notificationRepository;
    private final SmsGateway smsGateway;

    PaymentStatusNotificationListener(NotificationRepository notificationRepository, SmsGateway smsGateway) {
        this.notificationRepository = notificationRepository;
        this.smsGateway = smsGateway;
    }

    @ApplicationModuleListener
    void on(PaymentStatusChanged event) {
        String content = "Your payment of %s %s is now %s.".formatted(
                event.currencyCode(), event.amount(), event.newStatus());

        Notification notification = Notification.queue(
                event.farmerId(),
                Notification.Channel.SMS,
                Notification.Type.PAYMENT_CONFIRMATION,
                event.paymentId(),
                content);

        notificationRepository.save(notification);
        smsGateway.send(event.farmerId().toString(), content);
    }
}
