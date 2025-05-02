package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.dto.SongDTO;
import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.User;
import com.example.soundhiveapi.service.MyJdbcService;
import com.example.soundhiveapi.service.MyJdbcService.SongWithTags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Data endpoints: now returns SongDTO instead of raw Song.
 */
@RestController
@RequestMapping("/api")
public class DataController {

    @Autowired private MyJdbcService jdbcService;

    @GetMapping("/songs/{id}")
    public ResponseEntity<SongDTO> getSongById(@PathVariable int id, @RequestParam String userId) {
        SongWithTags swt = jdbcService.getSongWithTags(id);
        if (swt == null) {
            return ResponseEntity.notFound().build();
        }

        // convert tags to names
        List<String> tagNames = new ArrayList<>();
        for (Tag t : swt.tags) {
            tagNames.add(t.getTagName());
        }

        // build DTO with tag weights and duration
        SongDTO dto = new SongDTO(
                swt.song.getSongId(),
                swt.song.getTitle(),
                swt.song.getArtist(),
                tagNames,
                jdbcService.getTagWeightsForUser(userId, swt.tags),
                swt.song.getSongLength()
        );

        return ResponseEntity.ok(dto);
    }
}
