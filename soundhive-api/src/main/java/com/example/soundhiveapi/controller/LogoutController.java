package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.security.TokenBlacklist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class LogoutController {

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
        }
        String jwt = authHeader.substring(7);
        tokenBlacklist.revokeToken(jwt);
        return ResponseEntity.ok("Logged out successfully");
    }
}
