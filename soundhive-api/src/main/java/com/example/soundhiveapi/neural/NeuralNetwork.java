package com.example.soundhiveapi.neural;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orchestrates layers, forward pass, mini‑batch training, and prediction.
 */
public class NeuralNetwork {
    private final List<Layer> layers = new ArrayList<>();
    private final Optimizer   optimizer;

    public NeuralNetwork(int[] layerSizes, double learningRate) {
        // build hidden layers with ReLU; last layer with Sigmoid
        for (int i = 0; i < layerSizes.length - 1; i++) {
            ActivationFunction act = (i < layerSizes.length - 2)
                    ? ActivationFunction.RELU
                    : ActivationFunction.SIGMOID;
            layers.add(new DenseLayer(layerSizes[i], layerSizes[i + 1], act));
        }
        this.optimizer = new AdamOptimizer(learningRate);
    }

    /** Forward propagate input through all layers. */
    public double[] forward(double[] x) {
        double[] a = x;
        for (Layer layer : layers) {
            a = layer.forward(a);
        }
        return a;
    }

    /** Train on one mini‑batch of examples. */
    public void trainOnBatch(List<TrainingExample> batch) {
        int m = batch.size();

        // 1) initialize zeroed gradients for each layer
        List<LayerGradients> sumGrads = initZeroGradients();

        // 2) for each example: forward → compute dA → backward → accumulate
        for (TrainingExample ex : batch) {
            double[] yPred        = forward(ex.x);
            double[] dA           = Loss.crossEntropyDerivative(yPred, ex.y);
            List<LayerGradients> grads = backwardAll(dA);
            accumulateGradients(sumGrads, grads);
        }

        // 3) average the gradients over the batch
        averageGradients(sumGrads, m);

        // 4) apply the optimizer update
        optimizer.applyGradients(layers, sumGrads);
    }

    /** Predict scores (no training). */
    public double[] predict(double[] x) {
        return forward(x);
    }

    // ─── Helper methods ───────────────────────────────────────────────────────

    /** Run backward pass across all layers, returning per‑layer gradients. */
    private List<LayerGradients> backwardAll(double[] dA) {
        List<LayerGradients> grads   = new ArrayList<>();
        double[] currentDA = dA;

        // iterate layers in reverse
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer layer          = layers.get(i);
            LayerGradients layerG = layer.backward(currentDA);
            grads.add(0, layerG);              // prepend so index alignment remains
            currentDA = layerG.dAprev;         // feed into next (previous) layer
        }
        return grads;
    }

    /** Initialize a list of zero gradients matching each layer’s dimensions. */
    private List<LayerGradients> initZeroGradients() {
        List<LayerGradients> zeroList = new ArrayList<>();
        for (Layer layer : layers) {
            DenseLayer d = (DenseLayer) layer;
            int inSize  = d.getInputSize();
            int outSize = d.getOutputSize();

            double[][] zeroDW  = new double[inSize][outSize];
            double[]   zeroDB  = new double[outSize];
            double[]   zeroDAp = new double[inSize];

            zeroList.add(new LayerGradients(zeroDW, zeroDB, zeroDAp));
        }
        return zeroList;
    }

    /** Add the raw grads into the running sum. */
    private void accumulateGradients(List<LayerGradients> sum, List<LayerGradients> batchGrads) {
        for (int i = 0; i < sum.size(); i++) {
            LayerGradients s = sum.get(i);
            LayerGradients g = batchGrads.get(i);

            // accumulate dW
            for (int r = 0; r < s.dW.length; r++) {
                for (int c = 0; c < s.dW[0].length; c++) {
                    s.dW[r][c] += g.dW[r][c];
                }
            }
            // accumulate dB
            for (int j = 0; j < s.dB.length; j++) {
                s.dB[j] += g.dB[j];
            }
            // no need to accumulate dAprev here
        }
    }

    /** Divide each sum‑gradient by m (batch size) to get the average. */
    private void averageGradients(List<LayerGradients> sum, int m) {
        for (LayerGradients s : sum) {
            // average dW
            for (int r = 0; r < s.dW.length; r++) {
                for (int c = 0; c < s.dW[0].length; c++) {
                    s.dW[r][c] /= m;
                }
            }
            // average dB
            for (int j = 0; j < s.dB.length; j++) {
                s.dB[j] /= m;
            }
            // we don't use dAprev for updates
        }
    }

    // allow access to the ordered list of layers
    public List<Layer> getLayers() {
        return Collections.unmodifiableList(layers);
    }
}
