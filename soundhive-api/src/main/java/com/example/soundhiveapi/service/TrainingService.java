package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.neural.ModelSerializer;
import com.example.soundhiveapi.neural.TrainingExample;
import com.example.soundhiveapi.neural.NeuralNetwork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class TrainingService {

    @Autowired private MyJdbcService  jdbcService;
    @Autowired private NeuralNetwork  network;

    private final int batchSize = 5;
    private final int epochs    = 1;

    public void train() {
        List<TrainingExample> data = buildTrainingSet();
        Collections.shuffle(data);

        for (int e = 0; e < epochs; e++) {
            for (int i = 0; i < data.size(); i += batchSize) {
                int end = Math.min(i + batchSize, data.size());
                network.trainOnBatch(data.subList(i, end));
            }
        }
    }

    public void trainOnExample(TrainingExample example) {
        network.trainOnBatch(List.of(example));
    }

    public void saveModel() {
        try {
            ModelSerializer.saveModel(network);
            System.out.println("[Model Saved] Neural network weights saved to disk.");
        } catch (IOException e) {
            System.err.println("⚠ Error saving model: " + e.getMessage());
        }
    }

    private List<TrainingExample> buildTrainingSet() {
        double[][] matrix     = jdbcService.getUserTagWeightsMatrix();
        List<String> users    = jdbcService.getDistinctUserIds();
        List<Integer> songIds = jdbcService.getDistinctSongIds();
        int numSongs          = songIds.size();

        List<TrainingExample> examples = new ArrayList<>();

        for (int u = 0; u < users.size(); u++) {
            String userId    = users.get(u);
            double[] x       = matrix[u];
            double[] y       = new double[numSongs];

            Queue<Song> played = jdbcService.getUserPlayEvents(userId);
            for (Song s : played) {
                int songId = s.getSongId();
                for (int idx = 0; idx < numSongs; idx++) {
                    if (songIds.get(idx) == songId) {
                        y[idx] = 1.0;
                        break;
                    }
                }
            }

            examples.add(new TrainingExample(x, y));
        }

        return examples;
    }
}
