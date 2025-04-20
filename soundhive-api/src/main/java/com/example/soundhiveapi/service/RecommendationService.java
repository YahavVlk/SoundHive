package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.service.MyJdbcService;
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
     * Returns the top‑K song recommendations for a single user.
     */
    public List<Song> recommend(String userId, int topK) {
        // 1) Get the user’s feature vector
        double[] input = jdbcService.getUserTagWeightsArray(userId);

        // 2) Run the model
        double[] scores = network.predict(input);

        // 3) Use the same ID list as training: deterministic order
        List<Integer> songIds = jdbcService.getDistinctSongIds();

        // 4) Sort songIds by descending score
        List<Integer> sorted = new ArrayList<>(songIds);
        Collections.sort(sorted, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                // find index in the original list
                int idxA = songIds.indexOf(a);
                int idxB = songIds.indexOf(b);
                // compare corresponding scores
                return Double.compare(scores[idxB], scores[idxA]);
            }
        });

        // 5) Take top K and fetch Song entities
        List<Song> result = new ArrayList<>();
        for (int i = 0; i < topK && i < sorted.size(); i++) {
            int id = sorted.get(i);
            songRepo.findById(id).ifPresent(result::add);
        }
        return result;
    }
}
