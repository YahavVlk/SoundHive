package com.example.soundhiveapi;

import com.example.soundhiveapi.neural.ModelSerializer;
import com.example.soundhiveapi.neural.NeuralNetwork;
import com.example.soundhiveapi.service.FeatureUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;  // use Jakarta annotation for Spring Boot 3+

/**
 * Ensures both the neural‑network model and updated tag‑weights are persisted on shutdown.
 */
@Component
public class ShutdownManager {

    @Autowired private NeuralNetwork        neuralNetwork;
    @Autowired private FeatureUpdateService featureService;

    @PreDestroy
    public void onShutdown() {
        try {
            ModelSerializer.saveModel(neuralNetwork);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            featureService.flushToDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
