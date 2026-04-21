package com.pragun.ElectiSelect.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "session_id"})
})
public class OpenElectiveSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;

    private LocalDateTime selectedAt = LocalDateTime.now();
}
