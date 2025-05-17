package com.example.soundhiveapi.model;

import jakarta.persistence.*;

/**
 * Join table mapping songs to tags (Many-to-Many association)
 */
@Entity
@IdClass(SongTagId.class) // Composite primary key defined in separate ID class
@Table(name = "song_tag") // Table that links songs and tags
public class SongTag {

    @Id
    @Column(name = "song_id")
    private int songId; // Foreign key to Song

    @Id
    @Column(name = "tag_id")
    private int tagId;  // Foreign key to Tag

    // No extra columns — this is a pure association entity

    public SongTag() {} // Default constructor for JPA

    // Constructor for initializing both IDs
    public SongTag(int songId, int tagId) {
        this.songId = songId;
        this.tagId  = tagId;
    }

    // Getters
    public int getSongId() { return songId; }
    public int getTagId()  { return tagId; }
}
