package com.pragun.ElectiSelect.security;

import com.pragun.ElectiSelect.model.AcademicState;
import com.pragun.ElectiSelect.model.Role;
import com.pragun.ElectiSelect.model.User;
import com.pragun.ElectiSelect.repository.AcademicStateRepository;
import com.pragun.ElectiSelect.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final AcademicStateRepository academicStateRepository;

    public CustomOAuth2SuccessHandler(JwtUtils jwtUtils, UserRepository userRepository, AcademicStateRepository academicStateRepository) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.academicStateRepository = academicStateRepository;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // 🛡️ SECURITY LAYER 2: Domain Validation
        if (email == null ||!(email.endsWith("@dsce.edu.in") || email.endsWith("@dayanandasagar.edu"))) {
            getRedirectStrategy().sendRedirect(request, response, "http://localhost:5173/login?error=unauthorized_email");
            return;
        }

        //Registration logic
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            // Create new user if they don't exist
            User newUser = new User();
            newUser.setEmail(email);
            String rawName = oAuth2User.getAttribute("name");
            String cleanedName = rawName;
            
            if (rawName != null) {
                String[] parts = rawName.trim().split("\\s+");
                // If first part contains digits -> assume USN
                if (parts.length > 1 && parts[0].matches(".*\\d.*")) {
                    cleanedName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
                }
            }
            
            newUser.setName(cleanedName);
            newUser.setRole(Role.STUDENT); // Default

            User savedUser = userRepository.save(newUser);

            // Initialize Academic State (Rule: Manual update later)
            AcademicState state = new AcademicState();
            state.setUser(savedUser);
            state.setCurrentSemester(0); // Placeholder until Admin promotes them
            academicStateRepository.save(state);

            return savedUser;
        });

        // ✅ SUPER_ADMIN enforcement: always applied on every login, regardless of when user was created
        boolean isSuperAdminEmail = email.equals("santhosh-ise@dayanandasagar.edu");
        if (isSuperAdminEmail && user.getRole() != Role.SUPER_ADMIN) {
            user.setRole(Role.SUPER_ADMIN);
            userRepository.save(user);
        }

        String token = jwtUtils.generateToken(email, user.getRole().name());

        // Redirect to our Frontend (React) with the token in the URL
        // Once we build the frontend, it will grab this token from the URL and save it
        String targetUrl = "http://localhost:5173/login-success?token=" + token;

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}