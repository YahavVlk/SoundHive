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

    @Autowired
    private MyJdbcService jdbc;
    @Autowired
    private NeuralNetwork neuralNetwork;

    private final List<TrainingExample> buffer = new ArrayList<>(); // Buffer of examples for batch training
    private static final int BATCH_SIZE = 10;

    /**
     * Add a single training example to the buffer.
     * When the buffer reaches BATCH_SIZE, train the network on the batch.
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
     * Save the current state of the neural network to disk (model.dat).
     */
    public void saveModel() {
        try {
            ModelSerializer.saveModel(neuralNetwork);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Manually trigger training using the buffered examples.
     * Also clears the buffer and saves the model afterward.
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
     * Evaluate the prediction accuracy for a specific user.
     * Compares the top 20 predicted songs to the user's listening history.
     */
    public String evaluatePredictionAccuracy(String userId) {
        List<Integer> songIds = jdbc.getDistinctSongIds();
        neuralNetwork.setSongIdOrder(songIds); // Ensure output alignment

        // Get actual songs the user has listened to
        Set<Integer> history = jdbc.getUserPlayHistory(userId).stream()
                .map(UserPlayEvent::getSongId)
                .collect(Collectors.toSet());

        // Generate prediction scores from the model
        double[] input = jdbc.getUserTagWeightsArray(userId); // tag weight vector
        double[] predictions = neuralNetwork.predict(input);  // predicted song scores

        // Sort song IDs by predicted score descending
        List<Integer> ranked = new ArrayList<>(songIds);
        ranked.sort((a, b) -> Double.compare(
                predictions[songIds.indexOf(b)],
                predictions[songIds.indexOf(a)]));

        // Output top 20 predictions
        System.out.println("\n🎵 Top 20 predicted songs:");
        for (int i = 0; i < Math.min(20, ranked.size()); i++) {
            int songId = ranked.get(i);
            String title = jdbc.getSongTitle(songId);
            double score = predictions[songIds.indexOf(songId)];
            boolean inHistory = history.contains(songId);
            System.out.printf("%2d. %s (%.3f)%s%n", i + 1, title, score, inHistory ? " ✅" : "");
        }

        // Calculate accuracy as overlap with listening history
        long correct = ranked.stream().limit(20).filter(history::contains).count();
        double accuracy = correct / 20.0;
        return String.format("🎯 Accuracy for %s: %.2f%%", userId, accuracy * 100);
    }
}
