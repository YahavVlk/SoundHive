package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.service.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin endpoint to trigger training manually.
 */
@RestController
@RequestMapping("/api")
public class TrainingController {

    @Autowired private TrainingService trainingService;

    /**
     * POST /api/train
     * Runs one full mini‑batch training pass.
     */
    @PostMapping("/train")
    public ResponseEntity<String> trainOnce() {
        trainingService.train();
        return ResponseEntity.ok("Training completed");
    }
}
