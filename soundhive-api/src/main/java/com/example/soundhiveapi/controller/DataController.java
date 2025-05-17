package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.dto.SongDTO;
import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.User;
import com.example.soundhiveapi.repository.TagRepository;
import com.example.soundhiveapi.service.MyJdbcService;
import com.example.soundhiveapi.service.MyJdbcService.SongWithTags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Data endpoints: returns SongDTO instead of raw Song entity
 */
@RestController
@RequestMapping("/api")
public class DataController {

    @Autowired private MyJdbcService jdbcService;
    @Autowired private TagRepository tagRepository;

    @GetMapping("/songs/{id}")
    public ResponseEntity<SongDTO> getSongById(@PathVariable int id, @RequestParam String userId) {
        // Fetch song along with its tags from the DB
        SongWithTags swt = jdbcService.getSongWithTags(id);
        if (swt == null) {
            return ResponseEntity.notFound().build(); // Return 404 if song not found
        }

        // Extract tag names into a string list for DTO
        List<String> tagNames = new ArrayList<>();
        for (Tag t : swt.tags) {
            tagNames.add(t.getTagName());
        }

        // Construct DTO:
        // - songId, title, artist
        // - tag names
        // - tag weights specific to this user
        // - song length
        SongDTO dto = new SongDTO(
                swt.song.getSongId(),
                swt.song.getTitle(),
                swt.song.getArtist(),
                tagNames,
                jdbcService.getTagWeightsForUser(userId, swt.tags), // personalized weights
                swt.song.getSongLength()
        );

        return ResponseEntity.ok(dto); // Return DTO in JSON format
    }

    @GetMapping("/tags/all")
    public List<Tag> getAllTags() {
        return tagRepository.findAllByOrderByTagIdAsc();
    }

}
