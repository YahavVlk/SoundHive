package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.model.User;
import com.example.soundhiveapi.service.MyJdbcService;
import com.example.soundhiveapi.service.SimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes an endpoint to trigger programmatic simulation of listening sessions.
 */
@RestController
@RequestMapping("/api")
public class SimulationController {

    @Autowired private SimulationService sim;
    @Autowired private MyJdbcService     jdbc;

    /**
     * POST /api/simulate/{email}?n=10&p=0.2
     * Simulate `n` songs with feedback every `p` fraction of each song.
     *
     * @param email the user's email (we look up their idNumber internally)
     * @param n     number of songs to simulate
     * @param p     feedback interval as fraction (0 < p ≤ 1)
     */
    @PostMapping("/simulate/{email}")
    public ResponseEntity<String> simulate(
            @PathVariable String email,
            @RequestParam(name="n", defaultValue="10") int n,
            @RequestParam(name="p", defaultValue="0.2") double p
    ) {
        // 1) Lookup user by email
        User u = jdbc.getUserByEmail(email);
        if (u == null) {
            return ResponseEntity
                    .status(404)
                    .body("User not found: " + email);
        }

        // 2) Run the simulation using their internal idNumber
        sim.simulateSession(u.getIdNumber(), n, p);

        return ResponseEntity.ok(
                "Simulated " + n +
                        " songs for user " + email +
                        " with feedback interval " + p
        );
    }
}
