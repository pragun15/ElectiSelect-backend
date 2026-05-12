package com.pragun.ElectiSelect.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class AcademicState {
    @Id
    private Long userId; // Maps to User.id

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "current_semester")
    private int currentSemester;

    @Column(name = "is_eligible")
    private boolean isEligible = true;
}
