package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.service.FeatureUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FeedbackController {

    @Autowired private FeatureUpdateService featureUpdateService;

    // Request body structure for feedback submission
    public static class FeedbackRequest {
        public String  userId;      // ID of the user submitting feedback
        public int     songId;      // ID of the song
        public long    timestamp;   // Time listened in milliseconds (from start of song)
        public boolean isFavorite;  // Whether the song was liked/favorited
    }

    // Endpoint to receive feedback manually from user (e.g., liking a song or skipping)
    @PostMapping("/feedback")
    public ResponseEntity<String> receiveFeedback(@RequestBody FeedbackRequest req) {
        featureUpdateService.recordFeedback(
                req.userId,
                req.songId,
                req.timestamp,
                req.isFavorite,
                false // manually submitted feedback is never an "unfavorite"
        );
        return ResponseEntity.ok("Feedback recorded"); // Simple confirmation response
    }
}
