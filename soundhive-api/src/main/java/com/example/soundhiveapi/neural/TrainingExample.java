package com.example.soundhiveapi.neural;

import java.util.Map;

/**
 * Represents a single training example for the neural network.
 *
 * - x: input vector (user's tag weights)
 * - ySparse: sparse map of songId → feedback score
 *   (values usually range from 0.01 to 1.0 depending on listening percentage)
 */
public class TrainingExample {
    public final double[] x;                     // Input vector: tag weights
    public final Map<Integer, Double> ySparse;   // Sparse output: only songs with feedback

    public TrainingExample(double[] x, Map<Integer, Double> ySparse) {
        this.x = x;
        this.ySparse = ySparse;
    }
}
