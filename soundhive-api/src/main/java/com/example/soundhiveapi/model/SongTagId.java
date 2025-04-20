package com.example.soundhiveapi.model;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK for song–tag join table */
public class SongTagId implements Serializable {
    private int songId;
    private int tagId;

    public SongTagId() {}
    public SongTagId(int songId, int tagId) {
        this.songId = songId;
        this.tagId  = tagId;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SongTagId)) return false;
        SongTagId that = (SongTagId) o;
        return songId == that.songId && tagId == that.tagId;
    }
    @Override public int hashCode() {
        return Objects.hash(songId, tagId);
    }
}
