package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.User;
import com.example.soundhiveapi.model.UserTagWeight;
import com.example.soundhiveapi.repository.TagRepository;
import com.example.soundhiveapi.repository.UserRepository;
import com.example.soundhiveapi.repository.UserTagWeightRepository;
import com.example.soundhiveapi.security.JwtUtil;
import com.example.soundhiveapi.security.JdbcUserDetailsService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/auth") // ✅ Route prefix for all authentication endpoints
public class AuthController {

    @Autowired private JdbcUserDetailsService  userDetailsService;
    @Autowired private JwtUtil                 jwtUtil;
    @Autowired private UserRepository          userRepo;
    @Autowired private TagRepository           tagRepository;
    @Autowired private UserTagWeightRepository userTagWeightRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // ---------------------- LOGIN ----------------------

    // Data holder for login input (email + password)
    public static class LoginRequest {
        public String email;
        public String password;
    }

    // Response format holding the JWT token
    public static class JwtResponse {
        public String token;
        public JwtResponse(String token) { this.token = token; }
    }

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        User user = userRepo.findByEmail(req.email);
        if (user == null) {
            // No such user → reject
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        String storedPassword = user.getPassword();

        // Temporary patch: If stored password is not encrypted yet, encrypt and save it
        if ("secret123".equals(storedPassword)) {
            String encrypted = encoder.encode("secret123");
            user.setPassword(encrypted);
            userRepo.save(user);
            storedPassword = encrypted;
        }

        // Check password validity using BCrypt
        if (!encoder.matches(req.password, storedPassword)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        // Password correct → generate and return JWT token
        UserDetails ud = userDetailsService.loadUserByUsername(req.email);
        String token = jwtUtil.generateToken(ud);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    // ---------------------- REGISTER ----------------------

    // Request format for registration
    public static class RegisterRequest {
        @Size(min = 9, max = 9, message = "ID number must be exactly 9 digits")
        @Pattern(regexp = "\\d{9}", message = "ID number must be numeric")
        public String        idNumber;
        public String        username;
        public String        email;
        public String        password;
        public List<Integer> selectedTagIds; // Exactly 5 tags selected by user
    }

    // Response format to confirm created user
    public static class RegisterResponse {
        public String idNumber;
        public String username;
        public String email;
        public String password;
        public RegisterResponse(User u) {
            this.idNumber = u.getIdNumber();
            this.username = u.getUsername();
            this.email    = u.getEmail();
            this.password = u.getPassword();
        }
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {



        // Validate basic input
        if (req.username == null || req.username.isBlank() ||
                req.email == null || req.email.isBlank() ||
                req.password == null || req.password.length() < 6 ||
                req.selectedTagIds == null || req.selectedTagIds.size() != 5) {
            return ResponseEntity.badRequest()
                    .body("Must provide username, email, password≥6 chars, and exactly 5 tags");
        }

        // Reject if email already taken
        if (userRepo.findByEmail(req.email) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Email already in use");
        }

        // Reject if ID number already in use
        if (req.idNumber != null && !req.idNumber.isBlank()
                && userRepo.existsById(req.idNumber)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("idNumber already in use");
        }

        // Create new User object
        User u = new User();
        String id = req.idNumber != null && !req.idNumber.isBlank()
                ? req.idNumber
                : String.format("%09d", ThreadLocalRandom.current().nextInt(0, 1_000_000_000)); // Generate random 9-digit ID
        u.setIdNumber(id);
        u.setUsername(req.username);
        u.setEmail(req.email);
        u.setPassword(encoder.encode(req.password));
        userRepo.save(u);

        // Assign initial tag weights: 1.0 for selected tags, 0.05 for others
        List<Tag> allTags = tagRepository.findAllByOrderByTagIdAsc();
        List<UserTagWeight> weights = new ArrayList<>(allTags.size());
        for (Tag t : allTags) {
            double w = req.selectedTagIds.contains(t.getTagId()) ? 1.0 : 0.05;
            weights.add(new UserTagWeight(u.getIdNumber(), t.getTagId(), w));
        }
        userTagWeightRepository.saveAll(weights);

        // Return created user info
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(u));
    }
}
