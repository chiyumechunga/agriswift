package zm.agriswift.referencedata;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import zm.agriswift.common.BaseEntity;

@Entity
@Table(name = "depots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Depot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "depot_id")
    private Integer depotId;

    @Column(name = "depot_code", nullable = false, unique = true, length = 20)
    private String depotCode;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "district", length = 80)
    private String district;

    @Column(name = "province", length = 80)
    private String province;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Depot(String depotCode, String name, String district, String province) {
        this.depotCode = depotCode;
        this.name = name;
        this.district = district;
        this.province = province;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}