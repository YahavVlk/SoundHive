package com.example.soundhiveapi.model;

import java.io.Serializable;
import java.util.Objects;

public class UserTagWeightId implements Serializable {
    private String idNumber;
    private int tagId;

    public UserTagWeightId() {}

    public UserTagWeightId(String idNumber, int tagId) {
        this.idNumber = idNumber;
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserTagWeightId)) return false;
        UserTagWeightId that = (UserTagWeightId) o;
        return tagId == that.tagId &&
                Objects.equals(idNumber, that.idNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idNumber, tagId);
    }
}
