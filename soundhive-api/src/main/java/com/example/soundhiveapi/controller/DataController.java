package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.service.MyJdbcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DataController {

    @Autowired
    private MyJdbcService myJdbcService;

    // Endpoint to get all tags
    @GetMapping("/tags")
    public ResponseEntity<List<Tag>> getAllTags() {
        List<Tag> tags = myJdbcService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    // Endpoint to get a song by its ID
    @GetMapping("/songs/{id}")
    public ResponseEntity<Song> getSongById(@PathVariable int id) {
        Song song = myJdbcService.getSongById(id);
        if (song != null) {
            return ResponseEntity.ok(song);
        }
        return ResponseEntity.notFound().build();
    }

}
