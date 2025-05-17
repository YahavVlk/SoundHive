package com.example.soundhiveapi.neural;

import java.util.ArrayList;
import java.util.List;

/**
 * Adam optimizer implementation for training neural networks.
 * Uses momentum-based updates with bias correction.
 */
public class AdamOptimizer implements Optimizer {

    // Hyperparameters
    private final double lr;     // Learning rate
    private final double beta1;  // Decay rate for first moment (mean)
    private final double beta2;  // Decay rate for second moment (variance)
    private final double eps;    // Small constant for numerical stability
    private int t = 0;           // Time step (used for bias correction)

    // Moment vectors for weights and biases
    private final List<double[][]> mW = new ArrayList<>(); // First moment (mean) for weights
    private final List<double[]>   mB = new ArrayList<>(); // First moment for biases
    private final List<double[][]> vW = new ArrayList<>(); // Second moment (variance) for weights
    private final List<double[]>   vB = new ArrayList<>(); // Second moment for biases

    // Constructor with default beta1=0.9, beta2=0.999, eps=1e-8
    public AdamOptimizer(double learningRate) {
        this(learningRate, 0.9, 0.999, 1e-8);
    }

    /**
     * Full constructor for custom hyperparameters.
     */
    public AdamOptimizer(double lr, double beta1, double beta2, double eps) {
        this.lr    = lr;
        this.beta1 = beta1;
        this.beta2 = beta2;
        this.eps   = eps;
    }

    /**
     * Apply gradients to update each layer's weights and biases using Adam algorithm.
     */
    @Override
    public void applyGradients(List<Layer> layers, List<LayerGradients> grads) {
        // Initialize moment buffers only once (on first training step)
        if (mW.isEmpty()) {
            initMoments(layers);
        }

        // Increment time step (used for bias correction)
        t++;

        // Update weights and biases for each layer
        for (int l = 0; l < layers.size(); l++) {
            DenseLayer layer       = (DenseLayer) layers.get(l);
            LayerGradients grad    = grads.get(l);

            double[][] W           = layer.getWeights();
            double[]   B           = layer.getBiases();
            double[][] mWl         = mW.get(l);
            double[]   mBl         = mB.get(l);
            double[][] vWl         = vW.get(l);
            double[]   vBl         = vB.get(l);

            int inSize  = layer.getInputSize();
            int outSize = layer.getOutputSize();

            // --- Weight updates ---
            for (int i = 0; i < inSize; i++) {
                for (int j = 0; j < outSize; j++) {
                    // 1) Update biased first moment estimate (mean)
                    mWl[i][j] = beta1 * mWl[i][j] + (1 - beta1) * grad.dW[i][j];
                    // 2) Update biased second moment estimate (squared gradient)
                    vWl[i][j] = beta2 * vWl[i][j] + (1 - beta2) * grad.dW[i][j] * grad.dW[i][j];

                    // 3) Bias-corrected moment estimates
                    double mHat = mWl[i][j] / (1 - Math.pow(beta1, t));
                    double vHat = vWl[i][j] / (1 - Math.pow(beta2, t));

                    // 4) Update weights using Adam formula
                    W[i][j] -= lr * mHat / (Math.sqrt(vHat) + eps);
                }
            }

            // --- Bias updates ---
            for (int j = 0; j < outSize; j++) {
                // 1) First moment (mean)
                mBl[j] = beta1 * mBl[j] + (1 - beta1) * grad.dB[j];
                // 2) Second moment (variance)
                vBl[j] = beta2 * vBl[j] + (1 - beta2) * grad.dB[j] * grad.dB[j];

                // 3) Bias correction
                double mHat = mBl[j] / (1 - Math.pow(beta1, t));
                double vHat = vBl[j] / (1 - Math.pow(beta2, t));

                // 4) Update bias
                B[j] -= lr * mHat / (Math.sqrt(vHat) + eps);
            }
        }
    }

    /**
     * Initializes moment vectors (zeroed) for each layer's weights and biases.
     */
    private void initMoments(List<Layer> layers) {
        for (Layer layer : layers) {
            DenseLayer d     = (DenseLayer) layer;
            int inSize       = d.getInputSize();
            int outSize      = d.getOutputSize();
            mW.add(new double[inSize][outSize]);
            vW.add(new double[inSize][outSize]);
            mB.add(new double[outSize]);
            vB.add(new double[outSize]);
        }
    }
}
