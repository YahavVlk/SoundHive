package com.example.soundhiveapi.dto;

import java.util.List;

/**
 * A lightweight representation of a Song, including its parsed tags and tag weights.
 */
public class SongDTO {
    private final int          songId;
    private final String       title;
    private final String       artist;
    private final List<String> tags;
    private final List<Double> tagWeights;
    private final long         songLength;

    public SongDTO(int songId, String title, String artist,
                   List<String> tags, List<Double> tagWeights, long songLength) {
        this.songId     = songId;
        this.title      = title;
        this.artist     = artist;
        this.tags       = tags;
        this.tagWeights = tagWeights;
        this.songLength = songLength;
    }

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
