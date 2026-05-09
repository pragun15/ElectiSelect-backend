package com.pragun.ElectiSelect.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(
    name = "sessions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"type", "semester", "academic_year"})
)
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SessionType type;
    private int semester;
    @Column(name = "academic_year")
    private String academicYear;
    private Boolean isActive = false;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
