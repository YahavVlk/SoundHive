package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.UserPlayEvent;
import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.neural.NeuralNetwork;
import com.example.soundhiveapi.neural.TrainingExample;
import com.example.soundhiveapi.neural.ModelSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrainingService {

    @Autowired private MyJdbcService jdbc;
    @Autowired private NeuralNetwork neuralNetwork;
    @Autowired private RecommendationService recommendationService;

    private final List<TrainingExample> buffer = new ArrayList<>();
    private static final int BATCH_SIZE = 10;
    private static final double rarityWeightFactor = 0.4;

    public void trainOnExample(TrainingExample ex) {
        buffer.add(ex);
        System.out.println("[Trainer] Added training example. Current buffer size: " + buffer.size());

        if (buffer.size() >= BATCH_SIZE) {
            int numFeedbacks = jdbc.getUserPlayHistory(ex.getUserId()).size();
            int dynamicEpochs = numFeedbacks < 50 ? 15 : 10; // more epochs for early users
            neuralNetwork.setSongIdOrder(jdbc.getDistinctSongIds());
            neuralNetwork.setEpochs(dynamicEpochs);
            neuralNetwork.trainBatch(buffer);
            buffer.clear();

            System.out.println("🔁 [Trainer] Trained on batch of size " + BATCH_SIZE);
            logSampleWeights();

            saveModel();
        }
    }

    public void saveModel() {
        try {
            ModelSerializer.saveModel(neuralNetwork);
            System.out.println("✅ [Trainer] Neural network model saved.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void train() {
        neuralNetwork.setSongIdOrder(jdbc.getDistinctSongIds());
        if (!buffer.isEmpty()) {
            neuralNetwork.trainBatch(buffer);
            buffer.clear();
            logSampleWeights();
        }
        saveModel();
    }

    private void logSampleWeights() {
        double[] weights = neuralNetwork.getWeights();
        System.out.print("🔍 Sample weights: ");
        for (int i = 0; i < Math.min(10, weights.length); i++) {
            System.out.printf("%.4f ", weights[i]);
        }
        System.out.println("...");
    }

    /*
    * http://localhost:8080/api/train/evaluate?userId=110011001
    * */
    public double evaluate(String userId) {
        System.out.println("[Evaluate] Starting evaluation for user: " + userId);

        // Step 1: Show tag vector
        double[] tagVector = jdbc.getUserTagWeightsArray(userId);
        System.out.print("[Evaluate] Tag weights vector: ");
        for (double v : tagVector) {
            System.out.printf("%.4f ", v);
        }
        System.out.println();

        // Step 2: Use recommendAll to get actual ranked list
        List<Song> ranked = recommendationService.recommendAll(userId, Set.of());
        List<Integer> top50 = ranked.stream().limit(50).map(Song::getSongId).toList();

        System.out.println("[Evaluate] Top 50 recommended songs (full logic):");
        for (Song s : ranked.subList(0, Math.min(50, ranked.size()))) {
            System.out.printf("  %d → \"%s\"%n", s.getSongId(), s.getTitle());
        }

        // Step 3: Get filtered history (≥30% listened and not skipped)
        List<UserPlayEvent> historyEvents = jdbc.getUserPlayHistory(userId);
        Set<Integer> history = historyEvents.stream()
                .filter(e -> !e.isSkipped())
                .filter(e -> {
                    long ts = e.getId().getPlayTime().getTime();
                    long duration = jdbc.getSongDuration(e.getSongId());
                    double pct = Math.min(1.0, ts / (double) duration);
                    return pct >= 0.3;
                })
                .map(UserPlayEvent::getSongId)
                .collect(Collectors.toSet());

        System.out.println("[Evaluate] Filtered user history (≥30%% listened): " + history);

        if (history.isEmpty()) {
            System.out.println("[Evaluate] User has no valid history. Returning hit rate = 0.0");
            return 0.0;
        }

        // Step 4: Count hits
        List<Integer> hits = top50.stream().filter(history::contains).toList();
        System.out.println("[Evaluate] Matching songs in top 50:");
        System.out.println(hits);

        double hitRate = hits.size() / (double) history.size();
        System.out.printf("[Evaluate] Final hit rate: %.2f%n", hitRate);

        return hitRate;
    }

    public NeuralNetwork getNeuralNetwork() {
        return neuralNetwork;
    }
}
