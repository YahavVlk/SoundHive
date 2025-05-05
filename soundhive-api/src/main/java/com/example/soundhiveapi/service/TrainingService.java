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

    public void trainOnExample(TrainingExample ex) {
        buffer.add(ex);
        if (buffer.size() >= BATCH_SIZE) {
            neuralNetwork.trainBatch(buffer);
            buffer.clear();
        }
    }

    public void saveModel() {
        try {
            ModelSerializer.saveModel(neuralNetwork);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void train() {
        if (!buffer.isEmpty()) {
            neuralNetwork.trainBatch(buffer);
            buffer.clear();
        }
        saveModel();
    }

    public String evaluatePredictionAccuracy(String userId) {
        Set<Integer> history = jdbc.getUserPlayHistory(userId).stream()
                .map(UserPlayEvent::getSongId)
                .collect(Collectors.toSet());

        double[] input = jdbc.getUserTagWeightsArray(userId);
        double[] predictions = neuralNetwork.predict(input);
        List<Integer> songIds = jdbc.getDistinctSongIds();

        // Rank by predicted score
        List<Integer> ranked = new ArrayList<>(songIds);
        ranked.sort((a, b) -> Double.compare(
                predictions[songIds.indexOf(b)],
                predictions[songIds.indexOf(a)]));

        // Print top 20 predicted songs
        System.out.println("\n🎵 Top 20 predicted songs:");
        for (int i = 0; i < Math.min(20, ranked.size()); i++) {
            int songId = ranked.get(i);
            String title = jdbc.getSongTitle(songId);
            double score = predictions[songIds.indexOf(songId)];
            boolean inHistory = history.contains(songId);
            System.out.printf("%2d. %s (%.3f)%s%n", i + 1, title, score, inHistory ? " ✅" : "");
        }

        long correct = ranked.stream().limit(20).filter(history::contains).count();
        double accuracy = correct / 20.0;
        return String.format("🎯 Accuracy for %s: %.2f%%", userId, accuracy * 100);
    }
}
