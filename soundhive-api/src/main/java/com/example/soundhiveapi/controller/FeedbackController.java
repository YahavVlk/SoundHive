package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.service.FeatureUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FeedbackController {

    @Autowired private FeatureUpdateService featureUpdateService;

    public static class FeedbackRequest {
        public String  userId;
        public int     songId;
        public long    timestamp;     // ms since song start
        public boolean isFavorite;     // true if user “hearted” it
    }

    // POST /api/feedback
    @PostMapping("/feedback")
    public ResponseEntity<String> receiveFeedback(@RequestBody FeedbackRequest req) {
        featureUpdateService.recordFeedback(
                req.userId,
                req.songId,
                req.timestamp,
                req.isFavorite
        );
        return ResponseEntity.ok("Feedback recorded");
    }
}
