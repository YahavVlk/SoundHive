package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.service.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TrainingController {

    @Autowired
    private TrainingService trainingService;

    /**
     * POST /api/train?userId=some@email.com
     * Trains the model using buffered examples and returns evaluation result string.
     */
    @PostMapping("/train")
    public ResponseEntity<String> trainOnce(@RequestParam String userId) {
        trainingService.train();  // Trigger training on buffered examples
        double result = trainingService.evaluate(userId);  // Evaluate and return detailed result
        return ResponseEntity.ok("Training completed:\n" + result);
    }

    /**
     * GET /api/train/evaluate?userId=some@email.com
     * Evaluates the model's current predictions without additional training.
     */
    @GetMapping("/train/evaluate")
    public ResponseEntity<String> evaluateAccuracy(@RequestParam String userId) {
        double result = trainingService.evaluate(userId);  // Evaluate and return detailed result
        return ResponseEntity.ok("Evaluation only:\n" + result);
    }
}
