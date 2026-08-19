package zm.agriswift.referencedata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface CropPriceRepository extends JpaRepository<CropPrice, Long> {

    @org.springframework.data.jpa.repository.Query("""
            select p from CropPrice p
             where p.cropType.cropTypeId = :cropTypeId
               and p.effectiveFrom <= :asOf
               and (p.effectiveTo is null or p.effectiveTo > :asOf)
            """)
    Optional<CropPrice> findActivePrice(Short cropTypeId, Instant asOf);
}

