package com.example.soundhiveapi.config;

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

        return new NeuralNetwork(layerSizes, learningRate);
    }
}
