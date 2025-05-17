package com.example.soundhiveapi.neural;

/**
 * Represents a single neural network layer.
 * This interface defines the structure required for any layer implementation.
 */
public interface Layer {

    /**
     * Forward pass: calculates this layer's activations based on the
     * activations from the previous layer.
     *
     * @param aPrev activations from the previous layer
     * @return activations output by this layer
     */
    double[] forward(double[] aPrev);

    /**
     * Backward pass: calculates gradients with respect to the weights, biases,
     * and previous-layer activations using the chain rule.
     *
     * @param dA gradient with respect to this layer’s output activations
     * @return a container with gradients (weights, biases, and dA for previous layer)
     */
    LayerGradients backward(double[] dA);

    /**
     * Applies precomputed gradients to update this layer’s parameters (weights and biases).
     *
     * @param grads gradient container
     * @param learningRate learning rate to scale the update
     */
    void applyGradients(LayerGradients grads, double learningRate);
}
