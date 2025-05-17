package com.example.soundhiveapi.neural;

/**
 * Cross‑entropy loss and its derivative for training.
 * Assumes:
 * - yTrue is a one-hot or multi-hot binary vector (0 or 1)
 * - yPred contains predicted probabilities in range (0, 1)
 *
 * Derivative is simplified: dL/dA = yPred - yTrue
 */
public class Loss {

    /**
     * Compute the total cross‑entropy loss over all output units.
     * Formula: L = -Σ [ yTrueᵢ * log(yPredᵢ) ]
     *
     * @param yPred predicted probabilities
     * @param yTrue target binary values (0 or 1)
     * @return total loss over the output vector
     */
    public static double computeCrossEntropy(double[] yPred, double[] yTrue) {
        double loss = 0;
        for (int i = 0; i < yPred.length; i++) {
            // Clamp values to prevent log(0) or log(1) instability
            double p = Math.max(1e-15, Math.min(1 - 1e-15, yPred[i]));
            loss += - (yTrue[i] * Math.log(p));
        }
        return loss;
    }

    /**
     * Derivative of cross‑entropy loss with respect to activations.
     * Assumes sigmoid activation in final layer.
     *
     * @param yPred predicted probabilities
     * @param yTrue true binary labels
     * @return element-wise loss derivatives
     */
    public static double[] crossEntropyDerivative(double[] yPred, double[] yTrue) {
        double[] dA = new double[yPred.length];
        for (int i = 0; i < yPred.length; i++) {
            dA[i] = yPred[i] - yTrue[i]; // Simplified derivative
        }
        return dA;
    }
}
