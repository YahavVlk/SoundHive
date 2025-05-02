package com.example.soundhiveapi.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

public class UserPlayEventId implements Serializable {
    private String userId;
    private int songId;
    private Timestamp playTime;

    public UserPlayEventId() {}

    public UserPlayEventId(String userId, int songId, Timestamp playTime) {
        this.userId = userId;
        this.songId = songId;
        this.playTime = playTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPlayEventId)) return false;
        UserPlayEventId that = (UserPlayEventId) o;
        return songId == that.songId &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(playTime, that.playTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, songId, playTime);
    }
}
