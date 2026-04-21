package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.*;
import com.pragun.ElectiSelect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RegistrationService {

    private final SubjectRepository subjectRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    public RegistrationService(SubjectRepository subjectRepository, RegistrationRepository registrationRepository, UserRepository userRepository) {
        this.subjectRepository = subjectRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
    }

    @Transactional // 🛡️ CRITICAL: Ensures the entire operation succeeds or fails as one unit
    public void registerStudentForElective(String email, Long subjectId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (registrationRepository.existsByUser(user)) {
            throw new RuntimeException("You have already selected an elective. Selection is locked.");
        }

        // 1. Get Subject with a Database Lock
        Subject subject = subjectRepository.findByIdWithLock(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        // 2. Check Timing
        Session session = subject.getSession();
        if (!session.isActive() || LocalDateTime.now().isBefore(session.getStartTime()) || LocalDateTime.now().isAfter(session.getEndTime())) {
            throw new RuntimeException("Registration session is not currently open.");
        }

        // 3. Check Seat Availability
        if (subject.getFilled_seats()>= subject.getMaxSeats()) {
            throw new RuntimeException("No seats available in this elective.");
        }

        // 4. Update Seats and Save Registration
        subject.setFilled_seats(subject.getFilled_seats() + 1);
        subjectRepository.save(subject);

        Registration reg = new Registration();
        reg.setUser(user);
        reg.setSubject(subject);
        reg.setRegistrationTime(LocalDateTime.now());
        registrationRepository.save(reg);
    }

    public List<Registration> getAllRegistrations() {
        return registrationRepository.findAll();
    }

    public boolean hasStudentAlreadyRegistered(User user) {
        return registrationRepository.existsByUser(user);
    }
}