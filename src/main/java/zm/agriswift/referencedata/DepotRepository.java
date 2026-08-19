package zm.agriswift.referencedata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepotRepository extends JpaRepository<Depot, Integer> {

    /**
     * Finds a depot by its unique operational code (e.g., FRA-LUS-01).
     */
    Optional<Depot> findByDepotCode(String depotCode);

    /**
     * Checks if a depot code already exists (useful during creation validation).
     */
    boolean existsByDepotCode(String depotCode);

    /**
     * Returns all active depots (useful for UI dropdowns and mobile offline sync).
     */
    List<Depot> findAllByActiveTrue();

    /**
     * Finds all active depots within a specific province (e.g., "Lusaka", "Copperbelt").
     */
    List<Depot> findAllByProvinceAndActiveTrue(String province);

    /**
     * Finds all active depots within a specific district.
     */
    List<Depot> findAllByDistrictAndActiveTrue(String district);
}