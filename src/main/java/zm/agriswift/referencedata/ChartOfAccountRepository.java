package zm.agriswift.referencedata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccountEntry, String> {
}

