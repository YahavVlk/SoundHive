package com.example.soundhiveapi.model;

import jakarta.persistence.*;

@Entity
@Table(name = "songs")
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int songId;

    private String title;
    private String artist;

    @Column(name = "tags")
    private String Tags;

    private long songLength;  // in milliseconds

    // Getters and Setters

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

    public long getDuration() {
        return songLength;
    }
}
