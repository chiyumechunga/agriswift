package zm.agriswift.identity.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "roles")
public class Role {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Short roleId;

    @Getter
    @Column(name = "role_name", nullable = false, unique = true, length = 40)
    private String roleName;

    @Column(name = "description")
    private String description;

    protected Role() {
        // JPA
    }

    public Role(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

}
