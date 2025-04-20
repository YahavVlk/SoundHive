package com.example.soundhiveapi.neural;

import java.util.List;

/**
 * Interface for optimizers (e.g., Adam, SGD).
 */
public interface Optimizer {
    /**
     * Update each layer's parameters given their computed gradients.
     * @param layers list of layers in the network
     * @param grads  corresponding list of gradients
     */
    void applyGradients(List<Layer> layers, List<LayerGradients> grads);
}
