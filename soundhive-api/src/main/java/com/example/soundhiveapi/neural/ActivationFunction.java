package com.example.soundhiveapi.neural;

/**
 * Interface for activation functions and their derivatives.
 * Used in forward and backward passes of the neural network.
 */
public interface ActivationFunction {

    /**
     * Apply the activation to the linear input z (forward pass).
     */
    double apply(double z);

    /**
     * Compute the derivative of the activation at z (used in backpropagation).
     */
    double derivative(double z);

    // --- Predefined implementations below ---

    /**
     * ReLU activation: max(0, z)
     * - Common in hidden layers
     */
    ActivationFunction RELU = new ActivationFunction() {
        @Override
        public double apply(double z) {
            return Math.max(0, z);
        }

        @Override
        public double derivative(double z) {
            return z > 0 ? 1 : 0; // Derivative is 1 for z > 0, else 0
        }
    };

    /**
     * Sigmoid activation: 1 / (1 + e^-z)
     * - Often used in output layers for binary predictions
     */
    ActivationFunction SIGMOID = new ActivationFunction() {
        @Override
        public double apply(double z) {
            return 1.0 / (1 + Math.exp(-z));
        }

        @Override
        public double derivative(double z) {
            double s = 1.0 / (1 + Math.exp(-z));
            return s * (1 - s); // Derivative: sigmoid(z) * (1 - sigmoid(z))
        }
    };

    // Note: Softmax is not included here because it works on entire vectors,
    // not individual values like ReLU/Sigmoid.
}