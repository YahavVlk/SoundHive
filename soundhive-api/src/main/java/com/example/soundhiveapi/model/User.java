package com.example.soundhiveapi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "USERS")  // Make sure the table name matches your MySQL table
public class User {

    @Id
    private String idNumber; // Assuming idNumber is unique
    private String username;
    private String email;
    private String password; // You’ll need this for login

    // Constructors, getters, and setters
    public User() {
    }

    public User(String idNumber, String username, String email, String password) {
        this.idNumber = idNumber;
        this.username = username;
        this.email = email;
        this.password = password;
    }

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
