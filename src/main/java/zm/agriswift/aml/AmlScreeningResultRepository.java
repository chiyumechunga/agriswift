package zm.agriswift.aml;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AmlScreeningResultRepository extends JpaRepository<AmlScreeningResult, UUID> {
}