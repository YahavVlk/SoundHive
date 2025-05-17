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

    // Endpoint to revoke the JWT token on logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        // Check if Authorization header is present and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
        }

        // Extract the token from the header (remove "Bearer " prefix)
        String jwt = authHeader.substring(7);

        // Invalidate the token by adding it to the blacklist
        tokenBlacklist.revokeToken(jwt);

        return ResponseEntity.ok("Logged out successfully"); // Success response
    }
}
