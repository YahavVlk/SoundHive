package com.example.soundhiveapi.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key class for the SongTag entity (song_id + tag_id).
 * Must implement Serializable and override equals() and hashCode().
 */
public class SongTagId implements Serializable {

    private int songId; // Corresponds to the song_id column
    private int tagId;  // Corresponds to the tag_id column

    public SongTagId() {} // Default constructor required by JPA

    // Constructor to initialize both fields
    public SongTagId(int songId, int tagId) {
        this.songId = songId;
        this.tagId  = tagId;
    }

    // equals() method to compare composite key values
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SongTagId)) return false;
        SongTagId that = (SongTagId) o;
        return songId == that.songId && tagId == that.tagId;
    }

    // hashCode() based on both keys (required for HashMap, Set, etc.)
    @Override
    public int hashCode() {
        return Objects.hash(songId, tagId);
    }
}
