package com.pragun.ElectiSelect.security;


import com.pragun.ElectiSelect.model.Role;
import com.pragun.ElectiSelect.model.User;
import com.pragun.ElectiSelect.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Blocks access to all protected endpoints for users who haven't completed their profile.
 * Runs AFTER JwtAuthenticationFilter so the principal is already set.
 * Exempts: /api/user/me, /api/user/complete-profile, /api/admin/**, SUPER_ADMIN role.
 */
@Component
public class ProfileCompletionFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    public ProfileCompletionFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // Always allow: auth endpoints, profile completion endpoints, admin endpoints
        if (uri.startsWith("/auth/") || uri.startsWith("/login/") || uri.startsWith("/oauth2/")
                || uri.equals("/error")
                || uri.equals("/api/user/me")
                || uri.equals("/api/user/complete-profile")
                || uri.startsWith("/api/admin/")) {
            filterChain.doFilter(request, response);
            return;
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof String email)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        User user = userOpt.get();

        // SUPER_ADMIN bypasses profile completion
        if (user.getRole() == Role.SUPER_ADMIN) {
            filterChain.doFilter(request, response);
            return;
        }

        // Block if profile is not complete
        if (!user.isProfileCompleted()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"PROFILE_INCOMPLETE\", \"message\": \"Please complete your profile first.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
