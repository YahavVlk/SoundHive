package com.example.soundhiveapi.neural;

/**
 * Interface for activation functions and their derivatives.
 */
public interface ActivationFunction {
    /** Apply the activation to the linear input z. */
    double apply(double z);
    /** Compute the derivative of the activation at z (used in backprop). */
    double derivative(double z);

    // Predefined implementations:

    /** ReLU activation: max(0, z). */
    ActivationFunction RELU = new ActivationFunction() {
        @Override
        public double apply(double z) {
            return Math.max(0, z);
        }
        @Override
        public double derivative(double z) {
            return z > 0 ? 1 : 0;
        }
    };

    /** Sigmoid activation: 1 / (1 + e^-z). */
    ActivationFunction SIGMOID = new ActivationFunction() {
        @Override
        public double apply(double z) {
            return 1.0 / (1 + Math.exp(-z));
        }
        @Override
        public double derivative(double z) {
            double s = 1.0 / (1 + Math.exp(-z));
            return s * (1 - s);
        }
    };

    // Note: Softmax is applied over a vector of logits rather than per-element here.
}
