package zm.agriswift.referencedata;

import jakarta.persistence.*;

@Entity
@Table(name = "crop_types")
public class CropType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crop_type_id")
    private Short cropTypeId;

    @Column(name = "name", nullable = false, unique = true, length = 40)
    private String name;

    protected CropType() {
        // JPA
    }

    public Short getCropTypeId() {
        return cropTypeId;
    }

    public String getName() {
        return name;
    }
}
