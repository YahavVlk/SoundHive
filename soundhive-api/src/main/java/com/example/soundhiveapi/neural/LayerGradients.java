package com.example.soundhiveapi.neural;

/**
 * Container for gradients of one layer in the neural network.
 * Used during backpropagation to store:
 * - dW: ∂L/∂W — gradient of the loss w.r.t. weights
 * - dB: ∂L/∂b — gradient of the loss w.r.t. biases
 * - dAprev: ∂L/∂aᵢ₋₁ — gradient w.r.t. activations from the previous layer
 */
public class LayerGradients {
    public final double[][] dW;     // Weight gradients [inputSize][outputSize]
    public final double[]   dB;     // Bias gradients [outputSize]
    public final double[]   dAprev; // Gradients to propagate backward [inputSize]

    // Constructor to initialize all gradient arrays
    public LayerGradients(double[][] dW, double[] dB, double[] dAprev) {
        this.dW = dW;
        this.dB = dB;
        this.dAprev = dAprev;
    }
}
