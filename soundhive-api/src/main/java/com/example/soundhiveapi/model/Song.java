package com.example.soundhiveapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents a song in the system.
 * We store tags as a comma‑separated string in the rawTags column.
 */
@Entity
@Table(name = "SONGS")
public class Song {

    @Id
    @Column(name = "song_id")
    private int songId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "artist", nullable = false)
    private String artist;

    // the comma‑separated list of tag names, exactly as in your SQL inserts
    @Column(name = "tags", length = 255)
    private String rawTags;

    public Song() { }

    public Song(int songId, String title, String artist, String rawTags) {
        this.songId  = songId;
        this.title   = title;
        this.artist  = artist;
        this.rawTags = rawTags;
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

    public String getRawTags() {
        return rawTags;
    }
    public void setRawTags(String rawTags) {
        this.rawTags = rawTags;
    }

    @Override
    public String toString() {
        return "Song{" +
                "songId="   + songId +
                ", title='"  + title  + '\'' +
                ", artist='" + artist + '\'' +
                ", tags='"   + rawTags + '\'' +
                '}';
    }
}
