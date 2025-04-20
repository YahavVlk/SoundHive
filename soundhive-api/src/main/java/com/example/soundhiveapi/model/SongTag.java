package com.example.soundhiveapi.model;

import jakarta.persistence.*;

/** Join table mapping songs to tags */
@Entity
@IdClass(SongTagId.class)
@Table(name = "song_tag")
public class SongTag {
    @Id
    @Column(name = "song_id")
    private int songId;

    @Id @Column(name = "tag_id")
    private int tagId;

    // no payload other than the association

    public SongTag() {}
    public SongTag(int songId, int tagId) {
        this.songId = songId;
        this.tagId  = tagId;
    }
    public int getSongId() { return songId; }
    public int getTagId()  { return tagId; }
}
