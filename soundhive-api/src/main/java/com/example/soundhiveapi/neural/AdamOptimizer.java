package com.example.soundhiveapi.neural;

import java.util.ArrayList;
import java.util.List;

/**
 * Adam optimizer implementation.
 */
public class AdamOptimizer implements Optimizer {
    private final double lr;
    private final double beta1;
    private final double beta2;
    private final double eps;
    private int t = 0;

    // first and second moment vectors for weights and biases
    private final List<double[][]> mW = new ArrayList<>();
    private final List<double[]>   mB = new ArrayList<>();
    private final List<double[][]> vW = new ArrayList<>();
    private final List<double[]>   vB = new ArrayList<>();

    //Constructor with default beta1=0.9, beta2=0.999, eps=1e-8
    public AdamOptimizer(double learningRate) {
        this(learningRate, 0.9, 0.999, 1e-8);
    }

    /**
     * Full constructor.
     */
    public AdamOptimizer(double lr, double beta1, double beta2, double eps) {
        this.lr    = lr;
        this.beta1 = beta1;
        this.beta2 = beta2;
        this.eps   = eps;
    }

    /**
     * Apply gradients to update each layer's weights and biases.
     */
    @Override
    public void applyGradients(List<Layer> layers, List<LayerGradients> grads) {
        // initialize moment buffers on first call
        if (mW.isEmpty()) {
            initMoments(layers);
        }
        // increment time step
        t++;

        // for each layer l
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

            // update weights
            for (int i = 0; i < inSize; i++) {
                for (int j = 0; j < outSize; j++) {
                    // 1) update biased first moment estimate
                    mWl[i][j] = beta1 * mWl[i][j] + (1 - beta1) * grad.dW[i][j];
                    // 2) update biased second moment estimate
                    vWl[i][j] = beta2 * vWl[i][j] + (1 - beta2) * grad.dW[i][j] * grad.dW[i][j];

                    // 3) compute bias-corrected moments
                    double mHat = mWl[i][j] / (1 - Math.pow(beta1, t));
                    double vHat = vWl[i][j] / (1 - Math.pow(beta2, t));

                    // 4) update parameter
                    W[i][j] -= lr * mHat / (Math.sqrt(vHat) + eps);
                }
            }

            // update biases
            for (int j = 0; j < outSize; j++) {
                mBl[j] = beta1 * mBl[j] + (1 - beta1) * grad.dB[j];
                vBl[j] = beta2 * vBl[j] + (1 - beta2) * grad.dB[j] * grad.dB[j];

                double mHat = mBl[j] / (1 - Math.pow(beta1, t));
                double vHat = vBl[j] / (1 - Math.pow(beta2, t));

                B[j] -= lr * mHat / (Math.sqrt(vHat) + eps);
            }
        }
    }

    //Initialize zeroed moment vectors for each layer.
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
