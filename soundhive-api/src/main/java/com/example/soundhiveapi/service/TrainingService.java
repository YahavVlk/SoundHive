package com.example.soundhiveapi.service;

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

    /**
     * Adds a training example to the buffer.
     * Triggers batch training when BATCH_SIZE is reached.
     */
    public void trainOnExample(TrainingExample ex) {
        buffer.add(ex);
        if (buffer.size() >= BATCH_SIZE) {
            neuralNetwork.setSongIdOrder(jdbc.getDistinctSongIds());
            neuralNetwork.trainBatch(buffer);
            buffer.clear();
        }
    }

    /**
     * Saves the current model state to disk.
     */
    public void saveModel() {
        try {
            ModelSerializer.saveModel(neuralNetwork);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Manually triggers training with the current buffer and saves the model.
     */
    public void train() {
        neuralNetwork.setSongIdOrder(jdbc.getDistinctSongIds());
        if (!buffer.isEmpty()) {
            neuralNetwork.trainBatch(buffer);
            buffer.clear();
        }
        saveModel();
    }

    /**
     * Evaluates prediction accuracy for a user by comparing predicted top songs
     * to their actual listening history.
     */
    public String evaluatePredictionAccuracy(String userId) {
        List<Integer> songIds = jdbc.getDistinctSongIds();
        neuralNetwork.setSongIdOrder(songIds);

        Set<Integer> history = jdbc.getUserPlayHistory(userId).stream()
                .map(UserPlayEvent::getSongId)
                .collect(Collectors.toSet());

        double[] input = jdbc.getUserTagWeightsArray(userId);
        if (input == null || input.length == 0) return "User has no tag weights.";

        double[] predictions = neuralNetwork.predict(input);

        List<Integer> ranked = new ArrayList<>(songIds);
        Collections.sort(ranked, (a, b) -> Double.compare(
                predictions[songIds.indexOf(b)],
                predictions[songIds.indexOf(a)]));

        StringBuilder sb = new StringBuilder("\n🎵 Top 20 predicted songs:\n");
        for (int i = 0; i < Math.min(20, ranked.size()); i++) {
            int songId = ranked.get(i);
            String title = jdbc.getSongTitle(songId);
            double score = predictions[songIds.indexOf(songId)];
            boolean inHistory = history.contains(songId);
            sb.append(String.format("%2d. %s (%.3f)%s%n", i + 1, title, score, inHistory ? " ✅" : ""));
        }

        long correct = ranked.stream().limit(20).filter(history::contains).count();
        double accuracy = correct / 20.0;
        sb.append(String.format("Accuracy for %s: %.2f%%", userId, accuracy * 100));

        return sb.toString();
    }
}