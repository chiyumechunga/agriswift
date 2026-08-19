package zm.agriswift.systemconfig;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemConfigRepository extends JpaRepository<SystemConfigEntry, String> {
}
