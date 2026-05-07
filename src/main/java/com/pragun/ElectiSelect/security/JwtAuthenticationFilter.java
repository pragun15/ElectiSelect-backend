package com.pragun.ElectiSelect.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        System.out.println("🔍 Incoming Request to: " + request.getRequestURI());
        System.out.println("🔍 Authorization Header: " + (authHeader != null ? authHeader.substring(0, Math.min(20, authHeader.length())) + "..." : "NULL"));

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtils.validateToken(token)) {
                System.out.println("✅ Token is valid.");
                String email = jwtUtils.getEmailFromToken(token);
                String role = jwtUtils.getRoleFromToken(token); // 👈 Dynamically get the role

                // Spring Security requires the "ROLE_" prefix for hasRole checks
                String authority = "ROLE_" + role;

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        email, null, Collections.singletonList(new SimpleGrantedAuthority(authority)));

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("✅ Authentication set for user: " + email + " with role: " + authority);
            } else {
                System.out.println("❌ Token validation failed!");
                // Force a 401 response if the token is invalid or expired
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}