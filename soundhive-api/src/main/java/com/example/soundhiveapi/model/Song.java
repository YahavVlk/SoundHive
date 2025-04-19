package com.example.soundhiveapi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "SONGS")  // Ensure this matches your MySQL table name
public class Song {

    @Id
    private int songId; // Assuming each song has a unique identifier
    private String title;
    private String artist;

    // We can't directly store an array of Tag objects using JPA easily,
    // so we'll mark this field as @Transient and handle tag conversion separately,
    // or you might consider a separate join table if you need a relationship.
    @Transient
    private Tag[] tags; // exactly 5 tags

    public Song() {
    }

    public Song(int songId, String title, String artist, Tag[] tags) {
        if (tags == null || tags.length != 5) {
            throw new IllegalArgumentException("Exactly 5 tags are required.");
        }
        this.songId = songId;
        this.title = title;
        this.artist = artist;
        this.tags = tags;
    }

    public int getSongId() {
        return songId;
    }

    public void setSongId(int songId) {
        this.songId = songId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public Tag[] getTags() {
        return tags;
    }

    public void setTags(Tag[] tags) {
        if (tags == null || tags.length != 5) {
            throw new IllegalArgumentException("Exactly 5 tags are required.");
        }
        this.tags = tags;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Song{songId=").append(songId)
                .append(", title='").append(title).append('\'')
                .append(", artist='").append(artist).append('\'')
                .append(", tags=[");
        if (tags != null) {
            for (int i = 0; i < tags.length; i++) {
                sb.append(tags[i]);
                if (i < tags.length - 1) {
                    sb.append(", ");
                }
            }
        }
        sb.append("]}");
        return sb.toString();
    }
}
