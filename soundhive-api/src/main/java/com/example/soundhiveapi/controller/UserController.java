package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.model.User;
import com.example.soundhiveapi.service.MyJdbcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private MyJdbcService myJdbcService;

    // Login endpoint
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        User user = myJdbcService.getUserByEmail(loginRequest.getEmail());
        if (user != null && myJdbcService.checkPassword(user, loginRequest.getPassword())) {
            return ResponseEntity.ok(new UserDTO(user));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }
}

// DTO class for login request
class LoginRequest {
    private String email;
    private String password;

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}

// DTO class for user response
class UserDTO {
    private String idNumber;
    private String username;
    private String email;

    public UserDTO(User user) {
        this.idNumber = user.getIdNumber();
        this.username = user.getUsername();
        this.email = user.getEmail();
    }

    public String getIdNumber() {
        return idNumber;
    }
    public String getUsername() {
        return username;
    }
    public String getEmail() {
        return email;
    }
}
