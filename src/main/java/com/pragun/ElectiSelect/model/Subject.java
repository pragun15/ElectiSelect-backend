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

    // Comma-separated allowlist: if set (non-null, non-empty), ONLY these departments may select.
    // Takes precedence over restrictedDepts — workflow.md §9, §10 step 4.
    private String allowedDepts;

    // Comma-separated blocklist: departments blocked from selecting.
    // Only evaluated when allowedDepts is null/empty.
    private String restrictedDepts;
    @Column(name = "is_deleted")
    private boolean isDeleted;
}
