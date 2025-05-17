package com.example.soundhiveapi.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_playevents")
public class UserPlayEvent {

    @EmbeddedId
    private UserPlayEventId id;

    @Column(name = "song_title")
    private String title;

    @Column(name = "favorited")
    private boolean favorited;

    @Column(name = "skipped")
    private boolean skipped;

    public UserPlayEvent() {}

    public UserPlayEvent(UserPlayEventId id, boolean favorited, boolean skipped) {
        this.id = id;
        this.favorited = favorited;
        this.skipped = skipped;
    }

    public UserPlayEventId getId() {
        return id;
    }

    public void setId(UserPlayEventId id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public String getUserId() {
        return id.getUserId();
    }

    public int getSongId() {
        return id.getSongId();
    }

    public java.sql.Timestamp getPlayTime() {
        return id.getPlayTime();
    }
}
