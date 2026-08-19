package zm.agriswift.referencedata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, Short> {
    List<PaymentProvider> findByProviderTypeAndActiveTrue(PaymentProvider.ProviderType type);
}
