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
@RequestMapping("/api/auth") // ✅ FIXED HERE
public class AuthController {

    @Autowired private JdbcUserDetailsService  userDetailsService;
    @Autowired private JwtUtil                 jwtUtil;
    @Autowired private UserRepository          userRepo;
    @Autowired private TagRepository           tagRepository;
    @Autowired private UserTagWeightRepository userTagWeightRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // --- LOGIN ---

    public static class LoginRequest {
        public String email;
        public String password;
    }

    public static class JwtResponse {
        public String token;
        public JwtResponse(String token) { this.token = token; }
    }

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        User user = userRepo.findByEmail(req.email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        String storedPassword = user.getPassword();

        if ("secret123".equals(storedPassword)) {
            String encrypted = encoder.encode("secret123");
            user.setPassword(encrypted);
            userRepo.save(user);
            storedPassword = encrypted;
        }

        if (!encoder.matches(req.password, storedPassword)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        UserDetails ud = userDetailsService.loadUserByUsername(req.email);
        String token = jwtUtil.generateToken(ud);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    // --- REGISTER ---

    public static class RegisterRequest {
        @Size(min = 9, max = 9, message = "ID number must be exactly 9 digits")
        @Pattern(regexp = "\\d{9}", message = "ID number must be numeric")
        public String        idNumber;
        public String        username;
        public String        email;
        public String        password;
        public List<Integer> selectedTagIds; // exactly 5
    }

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
        if (req.username == null || req.username.isBlank() ||
                req.email == null || req.email.isBlank() ||
                req.password == null || req.password.length() < 6 ||
                req.selectedTagIds == null || req.selectedTagIds.size() != 5) {
            return ResponseEntity.badRequest()
                    .body("Must provide username, email, password≥6 chars, and exactly 5 tags");
        }

        if (userRepo.findByEmail(req.email) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Email already in use");
        }

        if (req.idNumber != null && !req.idNumber.isBlank()
                && userRepo.existsById(req.idNumber)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("idNumber already in use");
        }

        User u = new User();
        String id = req.idNumber != null && !req.idNumber.isBlank()
                ? req.idNumber
                : String.format("%09d", ThreadLocalRandom.current().nextInt(0, 1_000_000_000));
        u.setIdNumber(id);
        u.setUsername(req.username);
        u.setEmail(req.email);
        u.setPassword(encoder.encode(req.password));
        userRepo.save(u);

        List<Tag> allTags = tagRepository.findAllByOrderByTagIdAsc();
        List<UserTagWeight> weights = new ArrayList<>(allTags.size());
        for (Tag t : allTags) {
            double w = req.selectedTagIds.contains(t.getTagId()) ? 1.0 : 0.05;
            weights.add(new UserTagWeight(u.getIdNumber(), t.getTagId(), w));
        }
        userTagWeightRepository.saveAll(weights);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(u));
    }
}
