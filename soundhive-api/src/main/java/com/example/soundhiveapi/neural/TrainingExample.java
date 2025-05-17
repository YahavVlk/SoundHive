package com.example.soundhiveapi.neural;

import java.util.Map;

/**
 * Represents a single training example for the neural network.
 *
 * - userId: the user who generated this training example
 * - x: input vector (user's tag weights)
 * - ySparse: sparse map of songId → feedback score
 *   (values usually range from 0.01 to 1.0 depending on listening percentage)
 */
public class TrainingExample {
    public final String userId;                 // The user ID for this training example
    public final double[] x;                    // Input vector: tag weights
    public final Map<Integer, Double> ySparse;  // Sparse output: only songs with feedback

    public TrainingExample(String userId, double[] x, Map<Integer, Double> ySparse) {
        this.userId = userId;
        this.x = x;
        this.ySparse = ySparse;
    }

    public String getUserId() {
        return userId;
    }
}