package com.example.soundhiveapi.model;

import jakarta.persistence.*;

@Entity
@Table(name = "songs") // Maps this class to the "songs" table in the database
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment primary key
    private int songId;

    private String title;   // Song title
    private String artist;  // Artist name

    @Column(name = "tags")
    private String Tags;    // Comma-separated tag names as a string

    private long songLength;  // Song duration in milliseconds

    // --- Getters and Setters ---

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

    public String getTags() {
        return Tags;
    }

    public void setTags(String Tags) {
        this.Tags = Tags;
    }

    public long getSongLength() {
        return songLength;
    }

    public void setSongLength(long songLength) {
        this.songLength = songLength;
    }

    // Alias method (identical to getSongLength)
    public long getDuration() {
        return songLength;
    }
}
