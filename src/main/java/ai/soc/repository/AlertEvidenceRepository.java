package ai.soc.repository;

import ai.soc.entity.AlertEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertEvidenceRepository extends JpaRepository<AlertEvidence, Long> {
}