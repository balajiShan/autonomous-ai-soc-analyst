package ai.soc.repository;

import ai.soc.entity.AlertComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertCommentRepository extends JpaRepository<AlertComment, Long> {
}