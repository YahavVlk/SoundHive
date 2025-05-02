package com.example.soundhiveapi.security;

import com.example.soundhiveapi.model.User;
import com.example.soundhiveapi.service.MyJdbcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class JdbcUserDetailsService implements UserDetailsService {

    @Autowired
    private MyJdbcService jdbcService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = jdbcService.getUserByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("No user found for email: " + email);
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())   // must be BCrypt-hashed in DB
                .authorities("USER")
                .build();
    }
}
