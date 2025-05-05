package com.example.soundhiveapi.neural;

import java.util.Map;

/**
 * Training example where:
 * - x is the input tag-weight vector
 * - ySparse maps songId → feedback score (0.01 to 1.0)
 */
public class TrainingExample {
    public final double[] x;
    public final Map<Integer, Double> ySparse;

    public TrainingExample(double[] x, Map<Integer, Double> ySparse) {
        this.x = x;
        this.ySparse = ySparse;
    }
}
