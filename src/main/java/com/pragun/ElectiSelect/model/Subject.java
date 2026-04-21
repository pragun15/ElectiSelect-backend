package com.pragun.ElectiSelect.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;


    @ManyToOne
    @JoinColumn(name = "category_id") // Nullable because Open Electives don't have categories
    private DeptCategory category;

    @Column(unique = true, nullable = false)
    private String courseCode;
    private String title;
    private String department; // The offering department
    private int maxSeats;
    private int filled_seats = 0;

    // Store restricted departments as a simple comma-separated string for now
    private String restrictedDepts;
}
