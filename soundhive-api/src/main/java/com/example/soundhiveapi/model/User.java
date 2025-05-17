package com.example.soundhiveapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users") // Maps this entity to the "users" table
public class User {

    @Id
    @Column(name = "id_number", length = 9, nullable = false)
    @Size(min = 9, max = 9, message = "ID number must be exactly 9 digits")
    @Pattern(regexp = "\\d{9}", message = "ID number must contain only digits")
    private String idNumber; // National ID, used as primary key

    @Column(nullable = false)
    private String username; // Display name of the user

    @Column(nullable = false, unique = true)
    private String email;    // Must be unique across users

    @Column(nullable = false)
    private String password; // Hashed password (BCrypt)

    // --- Getters and Setters ---

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

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
