package com.example.soundhiveapi.dto;

import java.util.List;

/**
 * A lightweight representation of a Song, including its parsed tags.
 */
public class SongDTO {
    private final int          songId;
    private final String       title;
    private final String       artist;
    private final List<String> tags;

    public SongDTO(int songId, String title, String artist, List<String> tags) {
        this.songId = songId;
        this.title  = title;
        this.artist = artist;
        this.tags   = tags;
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
}
