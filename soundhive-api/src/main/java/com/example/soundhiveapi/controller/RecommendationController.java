package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.dto.SongDTO;
import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.UserPlayEvent;
import com.example.soundhiveapi.service.MyJdbcService;
import com.example.soundhiveapi.service.RecommendationService;
import com.example.soundhiveapi.service.MyJdbcService.SongWithTags;
import com.example.soundhiveapi.repository.UserPlayEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class RecommendationController {

    @Autowired private RecommendationService recService;
    @Autowired private MyJdbcService         jdbcService;
    @Autowired private UserPlayEventRepository playRepo;

    private final Map<String, Deque<Song>> userQueues = new HashMap<>();
    private static final int QUEUE_SIZE = 10;
    private static final int REFILL_THRESHOLD = 5;

    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<SongDTO>> getRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int k
    ) {
        userQueues.putIfAbsent(userId, new ArrayDeque<>());
        Deque<Song> queue = userQueues.get(userId);

        // Maintain recently played set to avoid repeats
        Set<Integer> recentlyPlayed = new HashSet<>();
        List<UserPlayEvent> history = playRepo.findTop20ByUserIdOrderByPlayTimeDesc(userId);
        history.forEach(e -> recentlyPlayed.add(e.getSongId()));

        // refill queue if needed
        if (queue.size() <= REFILL_THRESHOLD) {
            List<Song> batch = recService.recommend(userId, QUEUE_SIZE, recentlyPlayed);
            queue.addAll(batch);
        }

        // collect next K songs from queue
        List<SongDTO> dtos = new ArrayList<>();
        for (int i = 0; i < k && !queue.isEmpty(); i++) {
            Song song = queue.pollFirst();
            if (song == null) continue;

            SongWithTags swt = jdbcService.getSongWithTags(song.getSongId());
            if (swt == null) continue;

            List<String> tagNames = new ArrayList<>();
            for (Tag t : swt.tags) tagNames.add(t.getTagName());

            dtos.add(new SongDTO(
                    swt.song.getSongId(),
                    swt.song.getTitle(),
                    swt.song.getArtist(),
                    tagNames,
                    jdbcService.getTagWeightsForUser(userId, swt.tags),
                    swt.song.getSongLength()
            ));
        }

        return ResponseEntity.ok(dtos);
    }
}
