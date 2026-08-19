package zm.agriswift.referencedata;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "payment_providers")
public class PaymentProvider {

    public enum ProviderType {
        MOBILE_MONEY, BANK
    }

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id")
    private Short providerId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private ProviderType providerType;

    @Column(name = "provider_name", nullable = false, length = 60)
    private String providerName;

    @Getter
    @Column(name = "provider_code", nullable = false, unique = true, length = 20)
    private String providerCode;

    @Getter
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Getter
    @Column(name = "health_status", nullable = false, length = 20)
    private String healthStatus = "UNKNOWN";

    @Column(name = "avg_cost_bps")
    private Integer avgCostBps;

    protected PaymentProvider() {
        // JPA
    }

}
