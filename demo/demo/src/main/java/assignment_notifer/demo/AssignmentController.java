package assignment_notifer.demo;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssignmentController {

    private final AssignmentRepository assignmentRepository;

    public AssignmentController(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @GetMapping("/api/assignments")
    public List<Assignment> getAssignments() {
        return assignmentRepository.findAll();
    }

    @PostMapping("/api/assignments")
    public Assignment addAssignment(@RequestBody Assignment assignment) {
        return assignmentRepository.save(assignment);
    }
}