package com.example.soundhiveapi.neural;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orchestrates layers, forward pass, mini‑batch training, and prediction.
 */
public class NeuralNetwork {
    private final List<Layer> layers = new ArrayList<>();
    private final Optimizer optimizer;

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

    /** Predict scores (no training). */
    public double[] predict(double[] x) {
        return forward(x);
    }

    /** Train on one mini‑batch of examples. */
    public void trainOnBatch(List<TrainingExample> batch) {
        int m = batch.size();
        List<LayerGradients> sumGrads = initZeroGradients();

        for (TrainingExample ex : batch) {
            double[] yPred = forward(ex.x);
            double[] dA = Loss.crossEntropyDerivative(yPred, ex.y);
            List<LayerGradients> grads = backwardAll(dA);
            accumulateGradients(sumGrads, grads);
        }

        averageGradients(sumGrads, m);
        optimizer.applyGradients(layers, sumGrads);
    }

    /** Train on a single example (convenience wrapper). */
    public void trainOnExample(TrainingExample example) {
        trainOnBatch(List.of(example));
    }

    // ─── Helper methods ───────────────────────────────────────────────────────

    private List<LayerGradients> backwardAll(double[] dA) {
        List<LayerGradients> grads = new ArrayList<>();
        double[] currentDA = dA;

        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer layer = layers.get(i);
            LayerGradients layerG = layer.backward(currentDA);
            grads.add(0, layerG);
            currentDA = layerG.dAprev;
        }

        return grads;
    }

    private List<LayerGradients> initZeroGradients() {
        List<LayerGradients> zeroList = new ArrayList<>();
        for (Layer layer : layers) {
            DenseLayer d = (DenseLayer) layer;
            int inSize = d.getInputSize();
            int outSize = d.getOutputSize();
            zeroList.add(new LayerGradients(
                    new double[inSize][outSize],
                    new double[outSize],
                    new double[inSize]
            ));
        }
        return zeroList;
    }

    private void accumulateGradients(List<LayerGradients> sum, List<LayerGradients> batchGrads) {
        for (int i = 0; i < sum.size(); i++) {
            LayerGradients s = sum.get(i);
            LayerGradients g = batchGrads.get(i);

            for (int r = 0; r < s.dW.length; r++) {
                for (int c = 0; c < s.dW[0].length; c++) {
                    s.dW[r][c] += g.dW[r][c];
                }
            }
            for (int j = 0; j < s.dB.length; j++) {
                s.dB[j] += g.dB[j];
            }
        }
    }

    private void averageGradients(List<LayerGradients> sum, int m) {
        for (LayerGradients s : sum) {
            for (int r = 0; r < s.dW.length; r++) {
                for (int c = 0; c < s.dW[0].length; c++) {
                    s.dW[r][c] /= m;
                }
            }
            for (int j = 0; j < s.dB.length; j++) {
                s.dB[j] /= m;
            }
        }
    }

    public List<Layer> getLayers() {
        return Collections.unmodifiableList(layers);
    }
}
