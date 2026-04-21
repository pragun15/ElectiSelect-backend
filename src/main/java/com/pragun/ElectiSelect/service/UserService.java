package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.AcademicState;
import com.pragun.ElectiSelect.model.User;
import com.pragun.ElectiSelect.repository.AcademicStateRepository;
import com.pragun.ElectiSelect.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AcademicStateRepository academicStateRepository;

    public UserService(UserRepository userRepository, AcademicStateRepository academicStateRepository) {
        this.userRepository = userRepository;
        this.academicStateRepository = academicStateRepository;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public int getStudentSemester(String email) {
        User user = getUserByEmail(email);
        AcademicState state = academicStateRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Academic state not found for user: " + email));
        return state.getCurrentSemester();
    }
}