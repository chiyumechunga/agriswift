/**
 * Notification module: SMS/email delivery to farmers. Reacts to
 * {@link zm.agriswift.disbursement.api.PaymentStatusChanged} — the "Farmer
 * notified" box in disbursement_flow.png.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common", "farmer", "disbursement"}
)


package zm.agriswift.notification;