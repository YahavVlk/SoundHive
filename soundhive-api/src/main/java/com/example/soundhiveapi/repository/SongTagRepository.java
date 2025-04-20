package com.example.soundhiveapi.repository;

import com.example.soundhiveapi.model.SongTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SongTagRepository extends JpaRepository<SongTag, Object> {
    /** Find all tag‑IDs for a given song */
    List<SongTag> findBySongId(int songId);
}
