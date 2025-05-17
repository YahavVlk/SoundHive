package com.example.soundhiveapi;

import com.example.soundhiveapi.neural.ModelSerializer;
import com.example.soundhiveapi.neural.NeuralNetwork;
import com.example.soundhiveapi.service.FeatureUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;

/**
 * Ensures both the neural-network model and updated tag-weights
 * are persisted safely when the application shuts down.
 */
@Component
public class ShutdownManager {

    @Autowired private NeuralNetwork        neuralNetwork;     // Reference to trained model
    @Autowired private FeatureUpdateService featureService;    // For flushing user feedback data

    /**
     * Called automatically before the application shuts down.
     * Flushes user tag weight updates and saves the neural network to disk.
     */
    @PreDestroy
    public void onShutdown() {
        System.out.println("🛑 Graceful shutdown: saving user data and neural network...");

        try {
            featureService.flushToDb(); // Persist tag weights & recent feedback
            System.out.println("✅ User tag weights and recent plays flushed to DB.");
        } catch (Exception e) {
            System.err.println("❌ Failed to flush tag weights.");
            e.printStackTrace();
        }

        try {
            ModelSerializer.saveModel(neuralNetwork); // Save weights/biases to model.dat
            System.out.println("✅ Neural network model saved.");
        } catch (Exception e) {
            System.err.println("❌ Failed to save neural network.");
            e.printStackTrace();
        }
    }
}
