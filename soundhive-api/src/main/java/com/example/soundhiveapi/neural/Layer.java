package com.example.soundhiveapi.neural;

/**
 * Represents a single neural network layer.
 */
public interface Layer {
    /**
     * Forward pass: receive activations from previous layer and return current activations.
     * @param aPrev activations from previous layer
     * @return activations for this layer
     */
    double[] forward(double[] aPrev);

    /**
     * Backward pass: receive dA (gradient wrt this layer's outputs) and
     * return gradients for weights, biases, and previous-layer activations.
     * @param dA gradient wrt this layer's activations
     * @return gradients container
     */
    LayerGradients backward(double[] dA);

    void applyGradients(LayerGradients grads, double learningRate);
}
