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

    private int currentSemester;
    private boolean isEligible = true;
}
