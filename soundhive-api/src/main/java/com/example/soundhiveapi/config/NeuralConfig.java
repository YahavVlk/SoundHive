package com.example.soundhiveapi.config;

import com.example.soundhiveapi.neural.*;
import com.example.soundhiveapi.repository.TagRepository;
import com.example.soundhiveapi.repository.SongRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class NeuralConfig {

    private final TagRepository tagRepo;
    private final SongRepository songRepo;

    // Constructor injection of the tag and song repositories
    public NeuralConfig(TagRepository tagRepo, SongRepository songRepo) {
        this.tagRepo  = tagRepo;
        this.songRepo = songRepo;
    }

    /**
     * Defines and returns a NeuralNetwork bean with the following architecture:
     * Input size = number of tags
     *  → Hidden layer 1: 64 neurons with ReLU
     *  → Hidden layer 2: 32 neurons with ReLU
     *  → Output layer: number of songs with Sigmoid
     * Optimizer: Adam with learning rate = 0.001
     */
    @Bean
    public NeuralNetwork neuralNetwork() {
        int numTags  = (int) tagRepo.count();      // Get total number of tags from DB
        int numSongs = (int) songRepo.count();     // Get total number of songs from DB

        // Define the architecture of the neural network
        List<Layer> layers = new ArrayList<>();
        layers.add(new DenseLayer(numTags, 64, ActivationFunction.RELU));      // Input → Hidden 1
        layers.add(new DenseLayer(64, 32, ActivationFunction.RELU));           // Hidden 1 → Hidden 2
        layers.add(new DenseLayer(32, numSongs, ActivationFunction.SIGMOID));  // Hidden 2 → Output

        double learningRate = 0.001;
        NeuralNetwork net = new NeuralNetwork(layers, learningRate);

        try {
            // Attempt to load existing model weights from disk (model.dat)
            ModelSerializer.loadModel(net);
            System.out.println("[NeuralConfig] Loaded model weights from file.");
        } catch (Exception e) {
            // Fallback if file doesn't exist or loading fails — network starts fresh
            System.out.println("[NeuralConfig] No saved model found or failed to load. Starting fresh.");
        }

        // Optional debug print: biases from the first layer after loading weights
        System.out.println("[Debug] Biases after loading = " + Arrays.toString(((DenseLayer) net.getLayers().get(0)).getBiases()));

        net.setEpochs(10); // Default number of training epochs; can be changed elsewhere
        return net;
    }
}
