package com.example.soundhiveapi.neural;

/**
 * Container for gradients of one layer:
 * - dW: gradient wrt weights
 * - dB: gradient wrt biases
 * - dAprev: gradient wrt previous layer's activations
 */
public class LayerGradients {
    public final double[][] dW;
    public final double[]   dB;
    public final double[]   dAprev;

    public LayerGradients(double[][] dW, double[] dB, double[] dAprev) {
        this.dW = dW;
        this.dB = dB;
        this.dAprev = dAprev;
    }
}
