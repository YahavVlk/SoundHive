package com.example.soundhiveapi.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tags") // Maps this entity to the "tags" table in the database
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incremented primary key
    private int tagId;

    @Column(unique = true, nullable = false)
    private String tagName; // Unique, non-null name for the tag (e.g., "Rock", "Indie")

    // --- Getters and Setters ---

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
}
