package zm.agriswift.farmer.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import zm.agriswift.farmer.domain.KycDocument;

import java.util.UUID;

public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {
}
