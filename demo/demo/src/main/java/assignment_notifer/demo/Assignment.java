package assignment_notifer.demo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String canvasUid;

    private String course;
    private String name;
    private LocalDateTime dueDate;

    public Assignment() {
    }

    public Assignment(String course, String name, LocalDateTime dueDate) {
        this.course = course;
        this.name = name;
        this.dueDate = dueDate;
    }

    public Long getId() {
        return id;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
    public String getCanvasUid() {
    return canvasUid;
    }

    public void setCanvasUid(String canvasUid) {
        this.canvasUid = canvasUid;
    }
}