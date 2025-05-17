package com.example.soundhiveapi.dto;

import java.util.List;

/**
 * A lightweight representation of a Song, including:
 * - Parsed tag names
 * - Corresponding tag weights for the user
 * - Song metadata (id, title, artist, length)
 *
 * This class is used to transfer only the relevant song data to the frontend.
 */
public class SongDTO {
    private final int          songId;      // Unique song identifier
    private final String       title;       // Song title
    private final String       artist;      // Artist name
    private final List<String> tags;        // Tag names (e.g. ["Pop", "Indie"])
    private final List<Double> tagWeights;  // Tag weights specific to user preferences
    private final long         songLength;  // Duration of song in milliseconds

    // Constructor to initialize all fields
    public SongDTO(int songId, String title, String artist,
                   List<String> tags, List<Double> tagWeights, long songLength) {
        this.songId     = songId;
        this.title      = title;
        this.artist     = artist;
        this.tags       = tags;
        this.tagWeights = tagWeights;
        this.songLength = songLength;
    }

    // Getter methods for each field
    public int getSongId() {
        return songId;
    }
    public String getTitle() {
        return title;
    }
    public String getArtist() {
        return artist;
    }
    public List<String> getTags() {
        return tags;
    }
    public List<Double> getTagWeights() {
        return tagWeights;
    }
    public long getSongLength() {
        return songLength;
    }
}
