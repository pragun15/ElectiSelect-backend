package com.pragun.ElectiSelect.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"course_code", "session_id"})
})
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

    @Column(nullable = false)
    private String courseCode;
    private String title;
    private String department; // The offering department
    private int maxSeats;
    private int filled_seats = 0;

    @Column(name = "credits", nullable = false)
    private Integer credits;

    // Comma-separated blocklist: departments blocked from selecting.
    // Only evaluated when restrictedDepts is non-null and non-empty — workflow.md §9 step 4.
    private String restrictedDepts;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    // Legacy column from older schema — kept to satisfy DB NOT NULL constraint.
    // is_deleted is the authoritative field; this mirrors it.
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
}
