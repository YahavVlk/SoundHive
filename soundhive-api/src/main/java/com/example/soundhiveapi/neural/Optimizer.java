package com.example.soundhiveapi.neural;

import java.util.List;

/**
 * Interface for optimizers (e.g., Adam, SGD).
 * Optimizers update layer weights and biases based on gradients.
 */
public interface Optimizer {

    /**
     * Applies the gradients to the given layers.
     * Typically called after a full backward pass.
     *
     * @param layers list of neural network layers to update
     * @param grads  list of computed gradients (must align with layer order)
     */
    void applyGradients(List<Layer> layers, List<LayerGradients> grads);
}
