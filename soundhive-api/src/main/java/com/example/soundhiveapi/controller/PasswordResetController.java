package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.model.User;
import com.example.soundhiveapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PasswordResetController {

    @Autowired private UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Request format for password reset
    public static class ResetRequest {
        public String email;        // User's email address
        public String newPassword;  // New password to be set
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetRequest request) {
        // Validate request: email must be present and password at least 6 characters
        if (request.email == null || request.email.isBlank()
                || request.newPassword == null || request.newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("Email and new password (≥ 6 chars) are required.");
        }

        // Look up user by email
        User user = userRepository.findByEmail(request.email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No user found with email: " + request.email);
        }

        // Update user's password (hashed using BCrypt)
        user.setPassword(encoder.encode(request.newPassword));
        userRepository.save(user);

        return ResponseEntity.ok("Password reset successfully."); // Confirmation message
    }
}
