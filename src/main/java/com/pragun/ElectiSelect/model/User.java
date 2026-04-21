package com.pragun.ElectiSelect.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "app_users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(unique = true)
    private String usn;
    private String department;

    @Enumerated(EnumType.STRING)
    private Role role;
}