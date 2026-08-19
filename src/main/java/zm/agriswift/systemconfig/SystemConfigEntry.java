package zm.agriswift.systemconfig;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "system_config")
public class SystemConfigEntry {

    @Getter
    @Id
    @Column(name = "config_key", length = 80)
    private String configKey;

    @Getter
    @Column(name = "config_value", nullable = false)
    private String configValue;

    @Column(name = "description")
    private String description;

    protected SystemConfigEntry() {
        // JPA
    }

}

