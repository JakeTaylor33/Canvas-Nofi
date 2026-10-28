package assignment_notifer.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    Optional<Assignment> findByCanvasUid(String canvasUid);
}