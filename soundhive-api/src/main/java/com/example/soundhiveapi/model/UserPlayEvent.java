package com.example.soundhiveapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_playevents")
@IdClass(UserPlayEventId.class)
public class UserPlayEvent {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Id
    @Column(name = "song_id")
    private int songId;

    @Column(name = "play_time")
    private LocalDateTime playTime;

    // Optional: if your table has a song_title column
    @Column(name = "song_title")
    private String songTitle;

    public UserPlayEvent() { }

    public UserPlayEvent(String userId, int songId, LocalDateTime playTime, String songTitle) {
        this.userId = userId;
        this.songId = songId;
        this.playTime = playTime;
        this.songTitle = songTitle;
    }

    // Getters and setters:
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getSongId() {
        return songId;
    }

    public void setSongId(int songId) {
        this.songId = songId;
    }

    public LocalDateTime getPlayTime() {
        return playTime;
    }

    public void setPlayTime(LocalDateTime playTime) {
        this.playTime = playTime;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public void setSongTitle(String songTitle) {
        this.songTitle = songTitle;
    }
}
