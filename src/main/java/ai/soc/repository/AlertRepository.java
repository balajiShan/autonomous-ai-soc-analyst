package ai.soc.repository;

import ai.soc.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {

    @Query("SELECT COUNT(a) FROM Alert a")
    long countTotalAlerts();

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.validity = :validity")
    long countByValidity(String validity);

    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, a.alertCreationTime, a.lastUpdateTime)) FROM Alert a WHERE a.validity = :validity")
    Double findAverageAnalysisTimeByValidity(String validity);

    List<Alert> findTop10ByOrderByAlertCreationTimeDesc();

    @Query("SELECT a FROM Alert a WHERE (:keyword IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:severity IS NULL OR a.severity = :severity) " +
            "AND (:category IS NULL OR a.category = :category)")
    List<Alert> findByCriteria(@Param("keyword") String keyword, @Param("severity") String severity, @Param("category") String category);

}