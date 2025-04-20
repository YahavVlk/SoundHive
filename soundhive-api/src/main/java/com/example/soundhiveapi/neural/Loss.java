package com.example.soundhiveapi.neural;

/**
 * Cross‑entropy loss and its derivative for training.
 * Here we assume a one‑hot or multi‑hot target vector yTrue
 * and model outputs yPred in (0,1), so dL/dA = yPred - yTrue.
 */
public class Loss {

    /** Compute the total cross‑entropy loss over all output units. */
    public static double computeCrossEntropy(double[] yPred, double[] yTrue) {
        double loss = 0;
        for (int i = 0; i < yPred.length; i++) {
            // clamp to avoid log(0)
            double p = Math.max(1e-15, Math.min(1 - 1e-15, yPred[i]));
            loss += - (yTrue[i] * Math.log(p));
        }
        return loss;
    }

    /**
     * Derivative of cross‑entropy w.r.t. the activations (pre‑sigmoid/logit).
     * For a sigmoid + binary‑cross‑entropy setup, this simplifies to (yPred - yTrue).
     */
    public static double[] crossEntropyDerivative(double[] yPred, double[] yTrue) {
        double[] dA = new double[yPred.length];
        for (int i = 0; i < yPred.length; i++) {
            dA[i] = yPred[i] - yTrue[i];
        }
        return dA;
    }
}
