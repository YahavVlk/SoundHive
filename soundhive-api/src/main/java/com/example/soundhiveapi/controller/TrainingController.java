package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.service.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin endpoint to trigger training manually and evaluate model accuracy.
 */
@RestController
@RequestMapping("/api")
public class TrainingController {

    @Autowired private TrainingService trainingService;

    /**
     * POST /api/train?userId=some@email.com
     * Trains the model using all available data and evaluates prediction accuracy.
     */
    @PostMapping("/train")
    public ResponseEntity<String> trainOnce(@RequestParam String userId) {
        String result = trainingService.evaluatePredictionAccuracy(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/train/evaluate?userId=some@email.com
     * Returns prediction accuracy for that user's play history.
     */
    @PostMapping("/train/evaluate")
    public ResponseEntity<String> evaluateAccuracy(@RequestParam String userId) {
        String result = trainingService.evaluatePredictionAccuracy(userId);
        return ResponseEntity.ok(result);
    }
}
