package com.example.soundhiveapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@IdClass(UserPlayEventId.class)
@Table(name = "user_playevents")
public class UserPlayEvent {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Id
    @Column(name = "song_id")
    private int songId;

    @Id
    @CreationTimestamp
    @Column(name = "play_time", updatable = false)
    private Timestamp playTime;

    @Column(name = "song_title")
    private String songTitle;

    public UserPlayEvent() {}

    public UserPlayEvent(String userId, int songId, String songTitle) {
        this.userId = userId;
        this.songId = songId;
        this.songTitle = songTitle;
    }

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

    public Timestamp getPlayTime() {
        return playTime;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public void setSongTitle(String songTitle) {
        this.songTitle = songTitle;
    }

    public void setPlayTime(Timestamp playTime) {
        this.playTime = playTime;
    }
}
