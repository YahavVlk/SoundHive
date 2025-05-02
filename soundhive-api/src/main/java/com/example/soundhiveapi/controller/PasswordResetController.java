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

    public static class ResetRequest {
        public String email;
        public String newPassword;
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetRequest request) {
        if (request.email == null || request.email.isBlank()
                || request.newPassword == null || request.newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("Email and new password (≥ 6 chars) are required.");
        }

        User user = userRepository.findByEmail(request.email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No user found with email: " + request.email);
        }

        user.setPassword(encoder.encode(request.newPassword));
        userRepository.save(user);

        return ResponseEntity.ok("Password reset successfully.");
    }
}
