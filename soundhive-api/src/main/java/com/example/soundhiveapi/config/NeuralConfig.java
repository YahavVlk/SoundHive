package com.example.soundhiveapi.config;

import com.example.soundhiveapi.neural.ModelSerializer;
import com.example.soundhiveapi.neural.NeuralNetwork;
import com.example.soundhiveapi.repository.TagRepository;
import com.example.soundhiveapi.repository.SongRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NeuralConfig {

    private final TagRepository tagRepo;
    private final SongRepository songRepo;

    public NeuralConfig(TagRepository tagRepo, SongRepository songRepo) {
        this.tagRepo  = tagRepo;
        this.songRepo = songRepo;
    }

    /**
     * Expose a single NeuralNetwork bean with architecture:
     * [ numTags → 64 → 32 → numSongs ] and Adam lr=0.001
     */
    @Bean
    public NeuralNetwork neuralNetwork() {
        int numTags  = (int) tagRepo.count();
        int numSongs = (int) songRepo.count();

        int[] layerSizes    = new int[]{ numTags, 64, 32, numSongs };
        double learningRate = 0.001;

        NeuralNetwork net = new NeuralNetwork(layerSizes, learningRate);

        // Try loading saved weights (if the file exists)
        try {
            ModelSerializer.loadModel(net);
            System.out.println("[NeuralConfig] Loaded model weights from file.");
        } catch (Exception e) {
            System.out.println("[NeuralConfig] No saved model found or failed to load. Starting fresh.");
        }

        return net;
    }
}
