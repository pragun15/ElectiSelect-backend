package com.pragun.ElectiSelect.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class DeptCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private String categoryName;

    // Each category belongs to a specific session
    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    // This allows us to easily fetch all subjects belonging to this category
    // 'mappedBy' points to the category field we will add to the Subject entity
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<Subject> subjects;
}
