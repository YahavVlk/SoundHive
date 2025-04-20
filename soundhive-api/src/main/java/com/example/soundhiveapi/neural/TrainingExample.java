package com.example.soundhiveapi.neural;

/**
 * Simple container for one training example.
 * x = user tag‑weights vector (input)
 * y = one‑hot or multi‑hot vector of songs (target)
 */
public class TrainingExample {
    public final double[] x;
    public final double[] y;

    public TrainingExample(double[] x, double[] y) {
        this.x = x;
        this.y = y;
    }
}
