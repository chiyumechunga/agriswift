// src/main/java/zm/agriswift/disbursement/internal/ProviderCodes.java
package zm.agriswift.disbursement.internal;

import zm.agriswift.disbursement.domain.Payment;
import zm.agriswift.referencedata.PaymentProviderRepository;

public final class ProviderCodes {

    public static final String AIRTEL = "AIRTEL";

    private ProviderCodes() {
    }

    public static boolean isAirtel(PaymentProviderRepository providers, Payment payment) {
        return providers.findById(payment.getProviderId())
                .map(provider -> AIRTEL.equalsIgnoreCase(provider.getProviderCode()))
                .orElse(false);
    }
}