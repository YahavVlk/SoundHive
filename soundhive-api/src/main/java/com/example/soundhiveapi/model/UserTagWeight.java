package com.example.soundhiveapi.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "user_tagweights") // Maps this entity to the user_tagweights table
@IdClass(UserTagWeightId.class)  // Uses a composite key (idNumber + tagId)
public class UserTagWeight implements Serializable {

    @Id
    @Column(nullable = false)
    private String idNumber; // Foreign key to User (user ID number)

    @Id
    @Column(nullable = false)
    private int tagId; // Foreign key to Tag

    @Column(nullable = false)
    private double weight; // User's preference weight for this tag

    public UserTagWeight() {} // Default constructor required by JPA

    // Constructor to initialize all fields
    public UserTagWeight(String idNumber, int tagId, double weight) {
        this.idNumber = idNumber;
        this.tagId = tagId;
        this.weight = weight;
    }

    // --- Getters and Setters ---

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
