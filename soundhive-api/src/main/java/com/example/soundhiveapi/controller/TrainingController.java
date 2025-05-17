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
     * Trains the model using all available feedback data for the given user
     * and returns a prediction accuracy report.
     */
    @PostMapping("/train")
    public ResponseEntity<String> trainOnce(@RequestParam String userId) {
        // Evaluate prediction accuracy for the user (includes internal training logic)
        String result = trainingService.evaluatePredictionAccuracy(userId);
        return ResponseEntity.ok(result); // Return result string to frontend
    }

    /**
     * POST /api/train/evaluate?userId=some@email.com
     * Re-runs the prediction accuracy test without training again.
     */
    @PostMapping("/train/evaluate")
    public ResponseEntity<String> evaluateAccuracy(@RequestParam String userId) {
        // Same as above, but intended to be used just for evaluation
        String result = trainingService.evaluatePredictionAccuracy(userId);
        return ResponseEntity.ok(result); // Return prediction accuracy summary
    }
}
