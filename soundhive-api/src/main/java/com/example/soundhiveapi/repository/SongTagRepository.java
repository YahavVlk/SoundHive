package com.example.soundhiveapi.repository;

import com.example.soundhiveapi.model.SongTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repository for the SongTag join entity (song_id ↔ tag_id).
 */
public interface SongTagRepository extends JpaRepository<SongTag, Object> {

    /**
     * Find all SongTag records by songId.
     * @param songId the ID of the song
     * @return list of SongTag mappings for the song
     */
    List<SongTag> findBySongId(int songId);
}
