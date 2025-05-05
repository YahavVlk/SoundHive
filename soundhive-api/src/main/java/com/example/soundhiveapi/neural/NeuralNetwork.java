package com.example.soundhiveapi.neural;

import java.util.*;

public class NeuralNetwork {
    private final List<Layer> layers;
    private double learningRate;
    private int epochs = 10; // Number of epochs per batch

    public NeuralNetwork(List<Layer> layers, double learningRate) {
        this.layers = layers;
        this.learningRate = learningRate;
    }

    public double[] predict(double[] input) {
        double[] output = input;
        for (Layer layer : layers) {
            output = layer.forward(output);
        }
        return output;
    }

    public void trainBatch(List<TrainingExample> batch) {
        if (batch.isEmpty()) return;

        for (int epoch = 0; epoch < epochs; epoch++) {
            System.out.println("Epoch " + (epoch + 1) + "/" + epochs);

            int inputSize = batch.get(0).x.length;
            int outputSize = ((DenseLayer) layers.get(layers.size() - 1)).getOutputSize();

            List<LayerGradients> accumulated = new ArrayList<>();
            for (Layer layer : layers) {
                int in = ((DenseLayer) layer).getInputSize();
                int out = ((DenseLayer) layer).getOutputSize();
                accumulated.add(new LayerGradients(new double[in][out], new double[out], new double[in]));
            }

            for (TrainingExample example : batch) {
                System.out.println("🧠 Training Input (tag weights): " + Arrays.toString(example.x));
                System.out.println("🎯 Target Output (sparse): " + example.ySparse);

                double[] output = predict(example.x);

                double[] dLoss = new double[outputSize];
                for (Map.Entry<Integer, Double> entry : example.ySparse.entrySet()) {
                    int songIdIndex = entry.getKey();
                    double target = entry.getValue();
                    dLoss[songIdIndex] = output[songIdIndex] - target;
                }

                List<LayerGradients> grads = new ArrayList<>();
                double[] grad = dLoss;
                for (int i = layers.size() - 1; i >= 0; i--) {
                    Layer layer = layers.get(i);
                    LayerGradients g = layer.backward(grad);
                    grads.add(0, g);
                    grad = g.dAprev;
                }

                for (int i = 0; i < grads.size(); i++) {
                    LayerGradients g = grads.get(i);
                    LayerGradients acc = accumulated.get(i);
                    for (int j = 0; j < g.dW.length; j++) {
                        for (int k = 0; k < g.dW[0].length; k++) {
                            acc.dW[j][k] += g.dW[j][k];
                        }
                    }
                    for (int k = 0; k < g.dB.length; k++) {
                        acc.dB[k] += g.dB[k];
                    }
                }
            }

            for (int i = 0; i < layers.size(); i++) {
                DenseLayer layer = (DenseLayer) layers.get(i);
                LayerGradients g = accumulated.get(i);
                for (int j = 0; j < g.dW.length; j++) {
                    for (int k = 0; k < g.dW[0].length; k++) {
                        layer.getWeights()[j][k] -= learningRate * g.dW[j][k] / batch.size();
                    }
                }
                for (int k = 0; k < g.dB.length; k++) {
                    layer.getBiases()[k] -= learningRate * g.dB[k] / batch.size();
                }
            }
        }

        System.out.println("After training: First weight = " + ((DenseLayer) layers.get(0)).getWeights()[0][0]);
        System.out.println("[NeuralNetwork] Batch training complete.\n");
    }

    public void saveModel() {
        try {
            ModelSerializer.saveModel(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    public void setEpochs(int epochs) {
        this.epochs = epochs;
    }

    public List<Layer> getLayers() {
        return layers;
    }
}