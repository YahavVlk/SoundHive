package com.example.soundhiveapi.controller;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.model.User;
import com.example.soundhiveapi.dto.SongDTO;
import com.example.soundhiveapi.service.ListeningService;
import com.example.soundhiveapi.service.MyJdbcService;
import com.example.soundhiveapi.service.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/listening")
public class ListeningController {

    @Autowired private ListeningService listeningService;
    @Autowired private MyJdbcService jdbcService;
    @Autowired private TrainingService trainingService;

    @PostMapping("/start")
    public ResponseEntity<String> startListening(
            @RequestParam(defaultValue = "simulation") String mode,
            @RequestParam(defaultValue = "0.2") double interval,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        User user = jdbcService.getUserByEmail(email);
        String idNumber = user.getIdNumber();

        if (mode.equalsIgnoreCase("manual")) {
            new Thread(() -> listeningService.startManual(idNumber, interval)).start();
            return ResponseEntity.ok("Manual mode initialized for user: " + idNumber);
        } else {
            new Thread(() -> listeningService.startSimulation(idNumber, interval)).start();
            return ResponseEntity.ok("Simulation started for user: " + idNumber);
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopListening(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        System.out.println("[ListeningController] Stop requested by: " + email);

        User user = jdbcService.getUserByEmail(email);
        String idNumber = user.getIdNumber();

        listeningService.stopListening(idNumber);
        return ResponseEntity.ok("Stopped listening and saved model.");
    }

    @GetMapping("/manual/next")
    public ResponseEntity<SongDTO> getNextManualSong(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = jdbcService.getUserByEmail(email);
        String idNumber = user.getIdNumber();

        Song song = listeningService.getNextRecommendedSong(idNumber);
        if (song == null) return ResponseEntity.notFound().build();

        List<String> tags = Arrays.stream(song.getTags().split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .toList();

        List<Double> tagWeights = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++) tagWeights.add(0.0);

        SongDTO dto = new SongDTO(
                song.getSongId(),
                song.getTitle(),
                song.getArtist(),
                tags,
                tagWeights,
                song.getSongLength()
        );

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/manual/feedback")
    public ResponseEntity<Void> submitManualFeedback(
            @RequestParam(defaultValue = "false") boolean favorite,
            @RequestParam(defaultValue = "false") boolean unfavorite,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        User user = jdbcService.getUserByEmail(email);
        String idNumber = user.getIdNumber();

        listeningService.commitManualFeedback(idNumber, favorite, unfavorite);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/session/history")
    public ResponseEntity<List<SongDTO>> getSessionHistory(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = jdbcService.getUserByEmail(email);
        String idNumber = user.getIdNumber();

        List<Song> recentSongs = listeningService.getSessionHistoryForUser(idNumber);
        List<SongDTO> dtoList = new ArrayList<>();

        for (Song song : recentSongs) {
            List<String> tags = Arrays.stream(song.getTags().split(","))
                    .map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .toList();

            List<Double> tagWeights = new ArrayList<>();
            for (int i = 0; i < tags.size(); i++) tagWeights.add(0.0); // placeholder weights

            dtoList.add(new SongDTO(
                    song.getSongId(),
                    song.getTitle(),
                    song.getArtist(),
                    tags,
                    tagWeights,
                    song.getSongLength()
            ));
        }

        return ResponseEntity.ok(dtoList);
    }
}
