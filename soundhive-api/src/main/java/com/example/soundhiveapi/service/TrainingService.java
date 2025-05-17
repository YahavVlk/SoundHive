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

    private final List<TrainingExample> buffer = new ArrayList<>();
    private static final int BATCH_SIZE = 10;
    private static final double rarityWeightFactor = 0.4;

    public void trainOnExample(TrainingExample ex) {
        buffer.add(ex);
        System.out.println("[Trainer] Added training example. Current buffer size: " + buffer.size());

        if (buffer.size() >= BATCH_SIZE) {
            neuralNetwork.setSongIdOrder(jdbc.getDistinctSongIds());
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

    public double evaluate(String userId) {
        System.out.println("[Evaluate] Starting evaluation for user: " + userId);

        // Step 1: Show the tag weights vector
        double[] tagVector = jdbc.getUserTagWeightsArray(userId);
        System.out.print("[Evaluate] Tag weights vector: ");
        for (double v : tagVector) {
            System.out.printf("%.4f ", v);
        }
        System.out.println();

        // Step 2: Get predicted scores from the neural network
        Map<Integer, Double> predictedScores = jdbc.getPredictedScoreMap(userId);
        Map<Integer, String> songTitles = jdbc.getSongTitlesMap();

        System.out.println("[Evaluate] All predicted scores (songId → score → title):");
        predictedScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .forEach(entry -> {
                    int songId = entry.getKey();
                    double score = entry.getValue();
                    String title = songTitles.getOrDefault(songId, "Unknown");
                    System.out.printf("  %d → %.4f → \"%s\"%n", songId, score, title);
                });

        // Step 3: Sort and get top 50 predicted songs
        List<Integer> top50 = predictedScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(50)
                .map(Map.Entry::getKey)
                .toList();

        System.out.println("[Evaluate] Top 50 recommended song IDs:");
        System.out.println(top50);

        // Step 4: Get user recent history
        Set<Integer> history = new HashSet<>(jdbc.getRecentSongIds(userId));
        System.out.println("[Evaluate] User recent history song IDs:");
        System.out.println(history);

        if (history.isEmpty()) {
            System.out.println("[Evaluate] User has no song history. Returning hit rate of 0.");
            return 0.0;
        }

        // Step 5: Count hits
        List<Integer> hits = top50.stream().filter(history::contains).toList();
        long hitCount = hits.size();

        System.out.println("[Evaluate] Matching songs in history:");
        System.out.println(hits);

        double hitRate = hitCount / (double) history.size();
        System.out.printf("[Evaluate] Final hit rate: %.2f%n", hitRate);

        return hitRate;
    }
}
