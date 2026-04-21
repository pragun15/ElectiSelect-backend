package com.pragun.ElectiSelect.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SessionType type;
    private int semester;
    private String academicYear;
    private boolean isActive = false;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
