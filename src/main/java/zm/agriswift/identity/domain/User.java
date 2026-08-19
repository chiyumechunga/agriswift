package zm.agriswift.identity.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import zm.agriswift.common.BaseEntity;
import zm.agriswift.referencedata.Depot;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "external_subject", nullable = false, unique = true, length = 120)
    private String externalSubject;

    @Column(name = "username", nullable = false, length = 120)
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // NEW: link to farmer (for FRA officers/agents)
    @Column(name = "farmer_id")
    private UUID farmerId;

    // NEW: link to depot
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id")
    private Depot depot;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    protected User() { }

    public User(UUID userId, String externalSubject, String username, String email) {
        this.userId = userId;
        this.externalSubject = externalSubject;
        this.username = username;
        this.email = email;
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getRoleName().equals(roleName));
    }
}