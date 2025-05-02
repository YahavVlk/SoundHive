package com.example.soundhiveapi.repository;

import com.example.soundhiveapi.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SongRepository extends JpaRepository<Song, Integer> {
    // You can add custom queries here if needed in the future
}
