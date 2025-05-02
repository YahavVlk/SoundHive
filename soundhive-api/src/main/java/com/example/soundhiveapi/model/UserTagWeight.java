package com.example.soundhiveapi.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "user_tagweights")
@IdClass(UserTagWeightId.class)
public class UserTagWeight implements Serializable {

    @Id
    @Column(nullable = false)
    private String idNumber;

    @Id
    @Column(nullable = false)
    private int tagId;

    @Column(nullable = false)
    private double weight;

    public UserTagWeight() {}

    public UserTagWeight(String idNumber, int tagId, double weight) {
        this.idNumber = idNumber;
        this.tagId = tagId;
        this.weight = weight;
    }

    // Getters and Setters

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
