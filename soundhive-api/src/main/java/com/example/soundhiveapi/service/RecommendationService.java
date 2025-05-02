package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.neural.NeuralNetwork;
import com.example.soundhiveapi.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Runs inference on the trained network and returns top‑K song recommendations.
 */
@Service
public class RecommendationService {

    @Autowired private MyJdbcService   jdbcService;
    @Autowired private NeuralNetwork   network;
    @Autowired private SongRepository  songRepo;

    /**
     * Returns the top‑K song recommendations for a single user,
     * excluding songs from the user’s recent 20 play events.
     */
    public List<Song> recommend(String userId, int topK) {
        // 1) Get the user’s feature vector
        double[] input = jdbcService.getUserTagWeightsArray(userId);

        // 2) Run the model
        double[] scores = network.predict(input);

        // 3) Use the same ID list as training: deterministic order
        List<Integer> songIds = jdbcService.getDistinctSongIds();

        // 4) Filter out songs from recent 20 play events
        Set<Integer> recentlyPlayed = new HashSet<>();
        jdbcService.getUserPlayEvents(userId)
                .forEach(song -> recentlyPlayed.add(song.getSongId()));

        // 5) Sort songIds by descending score
        List<Integer> sorted = new ArrayList<>(songIds);
        Collections.sort(sorted, (a, b) -> {
            int idxA = songIds.indexOf(a);
            int idxB = songIds.indexOf(b);
            return Double.compare(scores[idxB], scores[idxA]);
        });

        // 6) Take top K that are not in recently played
        List<Song> result = new ArrayList<>();
        for (int id : sorted) {
            if (recentlyPlayed.contains(id)) continue;
            Optional<Song> songOpt = songRepo.findById(id);
            songOpt.ifPresent(result::add);
            if (result.size() == topK) break;
        }

        return result;
    }
}
