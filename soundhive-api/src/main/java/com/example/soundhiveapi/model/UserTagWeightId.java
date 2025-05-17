package com.example.soundhiveapi.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key class for UserTagWeight (idNumber + tagId).
 * Required by JPA to identify composite keys in @IdClass annotation.
 */
public class UserTagWeightId implements Serializable {

    private String idNumber; // User ID (foreign key to User)
    private int tagId;       // Tag ID (foreign key to Tag)

    public UserTagWeightId() {} // Required no-args constructor for JPA

    // Constructor for both fields
    public UserTagWeightId(String idNumber, int tagId) {
        this.idNumber = idNumber;
        this.tagId = tagId;
    }

    // Override equals() for value comparison
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserTagWeightId)) return false;
        UserTagWeightId that = (UserTagWeightId) o;
        return tagId == that.tagId &&
                Objects.equals(idNumber, that.idNumber);
    }

    // Override hashCode() to ensure uniqueness in hash-based collections
    @Override
    public int hashCode() {
        return Objects.hash(idNumber, tagId);
    }
}
