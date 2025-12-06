package ai.soc.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_comments")
@Data
public class AlertComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "alert_id", nullable = false)
    @JsonBackReference
    private Alert alert;

    @Column(name = "comment", columnDefinition = "TEXT", nullable = false)
    private String comment;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
}