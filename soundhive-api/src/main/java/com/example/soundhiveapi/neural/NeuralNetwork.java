package com.example.soundhiveapi.neural;

import java.util.*;

/**
 * Custom feedforward neural network with manual training logic.
 * Supports batch training using backpropagation and SGD.
 */
public class NeuralNetwork {
    private final List<Layer> layers;   // Network layers
    private double learningRate;        // Global learning rate
    private int epochs = 10;            // Number of training epochs

    private List<Integer> songIdOrder = new ArrayList<>(); // Maintains index-to-songId mapping for output layer

    public NeuralNetwork(List<Layer> layers, double learningRate) {
        this.layers = layers;
        this.learningRate = learningRate;
    }

    // Setter for the expected song ID order (used to map output neuron index → song ID)
    public void setSongIdOrder(List<Integer> songIds) {
        this.songIdOrder = new ArrayList<>(songIds);
    }

    // Run a forward pass through all layers
    public double[] predict(double[] input) {
        double[] output = input;
        for (Layer layer : layers) {
            output = layer.forward(output); // Feed through each layer
        }
        return output;
    }

    /**
     * Trains the model on a batch of examples for a fixed number of epochs.
     * Each example contains input tag weights and a sparse target (songId → score).
     */
    public void trainBatch(List<TrainingExample> batch) {
        if (batch.isEmpty()) return;

        for (int epoch = 0; epoch < epochs; epoch++) {
            System.out.println("Epoch " + (epoch + 1) + "/" + epochs);

            int inputSize = batch.get(0).x.length;
            int outputSize = ((DenseLayer) layers.get(layers.size() - 1)).getOutputSize();

            // Initialize zeroed accumulators for gradients across the batch
            List<LayerGradients> accumulated = new ArrayList<>();
            for (Layer layer : layers) {
                int in = ((DenseLayer) layer).getInputSize();
                int out = ((DenseLayer) layer).getOutputSize();
                accumulated.add(new LayerGradients(new double[in][out], new double[out], new double[in]));
            }

            for (TrainingExample example : batch) {
                System.out.println("🧠 Training Input (tag weights): " + Arrays.toString(example.x));
                System.out.println("🎯 Target Output (sparse): " + example.ySparse);

                double[] output = predict(example.x); // Forward pass

                // Compute sparse loss gradient: dLoss = yPred - yTrue for specified song IDs
                double[] dLoss = new double[outputSize];
                for (Map.Entry<Integer, Double> entry : example.ySparse.entrySet()) {
                    int songId = entry.getKey();
                    int songIndex = songIdOrder.indexOf(songId); // Find correct output index
                    if (songIndex >= 0 && songIndex < dLoss.length) {
                        dLoss[songIndex] = output[songIndex] - entry.getValue();
                    }
                }

                // Backpropagate through layers (in reverse order)
                List<LayerGradients> grads = new ArrayList<>();
                double[] grad = dLoss;
                for (int i = layers.size() - 1; i >= 0; i--) {
                    Layer layer = layers.get(i);
                    LayerGradients g = layer.backward(grad);
                    grads.add(0, g); // prepend to maintain order
                    grad = g.dAprev;
                }

                // Accumulate gradients from all examples
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

            // Apply average of accumulated gradients to update weights and biases
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

        // Print diagnostic info after training
        System.out.println("After training: First weight = " + ((DenseLayer) layers.get(0)).getWeights()[0][0]);
        System.out.println("[NeuralNetwork] Batch training complete.\n");
    }

    // Save model weights to disk
    public void saveModel() {
        try {
            ModelSerializer.saveModel(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Getters & Setters ---

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

    public List<Integer> getSongIdOrder() {
        return songIdOrder;
    }

    public double[] getWeights() {
        List<Double> allWeights = new ArrayList<>();
        for (Layer layer : layers) {
            if (layer instanceof DenseLayer dense) {
                double[][] w = dense.getWeights();
                for (double[] row : w) {
                    for (double val : row) {
                        allWeights.add(val);
                    }
                }
            }
        }
        // Convert List<Double> to double[]
        double[] flat = new double[allWeights.size()];
        for (int i = 0; i < allWeights.size(); i++) {
            flat[i] = allWeights.get(i);
        }
        return flat;
    }
}
