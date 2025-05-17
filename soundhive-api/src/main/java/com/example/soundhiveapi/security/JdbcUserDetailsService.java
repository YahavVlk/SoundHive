package com.example.soundhiveapi.security;

import com.example.soundhiveapi.model.User;
import com.example.soundhiveapi.service.MyJdbcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class JdbcUserDetailsService implements UserDetailsService {

    @Autowired
    private MyJdbcService jdbcService; // Custom JDBC-based service to fetch user data

    /**
     * Load user details by email (used during authentication).
     * Spring Security calls this when validating JWT credentials.
     *
     * @param email the email of the user trying to authenticate
     * @return UserDetails object with email, hashed password, and authorities
     * @throws UsernameNotFoundException if the user is not found
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = jdbcService.getUserByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("No user found for email: " + email);
        }

        // Build a Spring Security-compatible user object with role "USER"
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())   // Must be a BCrypt-hashed password
                .authorities("USER")
                .build();
    }
}
