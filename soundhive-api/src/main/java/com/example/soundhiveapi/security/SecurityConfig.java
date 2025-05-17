package com.example.soundhiveapi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JdbcUserDetailsService userDetailsService;
    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(JdbcUserDetailsService uds, JwtRequestFilter jwtRequestFilter) {
        this.userDetailsService = uds;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    /**
     * Main Spring Security configuration for the HTTP request filtering chain.
     * - Disables CSRF (stateless API)
     * - Enables JWT authentication
     * - Sets endpoints access permissions
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless APIs
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // No HTTP session created
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no authentication required
                        .requestMatchers(
                                "/api/auth/**",           // Login/Register
                                "/api/tags/**",              // Fetch tags
                                "/api/reset-password",    // Reset password
                                "/api/train/**"           // Training and evaluation endpoints
                        ).permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                // Insert JWT filter before Spring’s built-in username/password filter
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults()); // Support for basic auth if needed (optional)

        return http.build(); // Return the configured filter chain
    }

    /**
     * Configures the AuthenticationManager used by Spring Security.
     * It uses the custom JdbcUserDetailsService with BCrypt for password matching.
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
        return authBuilder.build();
    }

    /**
     * Bean for password encoding using BCrypt (secure and recommended).
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
