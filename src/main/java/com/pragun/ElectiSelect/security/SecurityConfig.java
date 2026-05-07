package com.pragun.ElectiSelect.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableMethodSecurity
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final CustomOAuth2SuccessHandler successHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ProfileCompletionFilter profileCompletionFilter;

    public SecurityConfig(CustomOAuth2SuccessHandler successHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          ProfileCompletionFilter profileCompletionFilter) {
        this.successHandler = successHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.profileCompletionFilter = profileCompletionFilter;
    }



    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOrigins(java.util.List.of("http://localhost:3000", "http://localhost:5173"));
                    config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(java.util.List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 🛡️ THE GATEKEEPER
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/login/**", "/oauth2/**", "/error").permitAll() // 👈 MUST HAVE /error
                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2.successHandler(successHandler))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                );

        // 🛡️ THE JWT GUARD
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        // 🛡️ PROFILE COMPLETION GUARD (runs after JWT so principal is set)
        http.addFilterAfter(profileCompletionFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}