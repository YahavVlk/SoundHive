package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.model.User;
import com.example.soundhiveapi.service.ListeningService;
import com.example.soundhiveapi.service.MyJdbcService;
import com.example.soundhiveapi.service.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/listening")
public class ListeningController {

    @Autowired private ListeningService listeningService;
    @Autowired private TrainingService  trainingService;
    @Autowired private MyJdbcService    jdbcService;

    /**
     * Starts either simulation or manual listening mode for the user.
     * The mode is controlled from frontend via query param (?mode=manual or ?mode=simulation).
     */
    @PostMapping("/start")
    public ResponseEntity<String> startListening(
            @RequestParam(defaultValue = "simulation") String mode,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        User user = jdbcService.getUserByEmail(email);
        String idNumber = user.getIdNumber();

        if (mode.equalsIgnoreCase("manual")) {
            return ResponseEntity.ok("Manual mode initialized for user: " + idNumber);
        } else {
            new Thread(() -> listeningService.startSimulation(idNumber)).start();
            return ResponseEntity.ok("Simulation started for user: " + idNumber);
        }
    }

    /**
     * Stops listening session and saves the neural network model to disk.
     */
    @PostMapping("/stop")
    public ResponseEntity<String> stopListening(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = jdbcService.getUserByEmail(email);
        String idNumber = user.getIdNumber();

        listeningService.stopSimulation(idNumber);
        trainingService.saveModel();
        return ResponseEntity.ok("Stopped listening and saved model.");
    }
}
