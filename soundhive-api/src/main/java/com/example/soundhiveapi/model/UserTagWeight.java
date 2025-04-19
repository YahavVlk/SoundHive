package com.example.soundhiveapi.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_tagweights")
@IdClass(UserTagWeightId.class)
public class UserTagWeight {

    @Id
    @Column(name = "id_number")
    private String idNumber;

    @Id
    @Column(name = "tag_id")
    private int tagId;

    @Column(name = "weight")
    private double weight;

    public UserTagWeight() { }

    public UserTagWeight(String idNumber, int tagId, double weight) {
        this.idNumber = idNumber;
        this.tagId = tagId;
        this.weight = weight;
    }

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
