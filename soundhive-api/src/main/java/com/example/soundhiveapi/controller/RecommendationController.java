package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.dto.SongDTO;
import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.service.MyJdbcService;
import com.example.soundhiveapi.service.RecommendationService;
import com.example.soundhiveapi.service.MyJdbcService.SongWithTags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Returns top‑K recommendations as DTOs (with tag names).
 */
@RestController
@RequestMapping("/api")
public class RecommendationController {

    @Autowired private RecommendationService recService;
    @Autowired private MyJdbcService         jdbcService;

    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<SongDTO>> getRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int k
    ) {
        // 1) get the recommended Song entities
        List<Integer> songIds = new ArrayList<>();
        recService.recommend(userId, k)
                .forEach(song -> songIds.add(song.getSongId()));

        // 2) map each to SongWithTags → SongDTO
        List<SongDTO> dtos = new ArrayList<>();
        for (Integer id : songIds) {
            SongWithTags swt = jdbcService.getSongWithTags(id);
            if (swt == null) continue;
            // extract tag names
            List<String> tagNames = new ArrayList<>();
            for (Tag t : swt.tags) {
                tagNames.add(t.getTagName());
            }
            dtos.add(new SongDTO(
                            swt.song.getSongId(),
                            swt.song.getTitle(),
                            swt.song.getArtist(),
                            tagNames,
                            jdbcService.getTagWeightsForUser(userId, swt.tags),
                            swt.song.getSongLength()
                    )
            );

        }
        return ResponseEntity.ok(dtos);
    }
}
