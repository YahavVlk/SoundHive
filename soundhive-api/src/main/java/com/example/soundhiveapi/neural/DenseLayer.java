package com.example.soundhiveapi.neural;

import java.util.Random;

/**
 * Fully‑connected (dense) layer implementation.
 * Each neuron is connected to every input.
 */
public class DenseLayer implements Layer {

    private final int inputSize, outputSize;              // Layer dimensions
    private final ActivationFunction act;                 // Activation function (e.g. ReLU, Sigmoid)
    private final double[][] weights;                     // Weight matrix [inputSize][outputSize]
    private final double[] biases;                        // Bias vector [outputSize]

    // Stored during forward pass — used in backpropagation
    private double[] lastInput; // input to the layer (aPrev)
    private double[] lastZ;     // raw outputs before activation (z)

    // Constructor: defines the structure and initializes parameters
    public DenseLayer(int inputSize, int outputSize, ActivationFunction act) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.act = act;
        this.weights = new double[inputSize][outputSize];
        this.biases  = new double[outputSize];
        initializeParameters(); // Random weight init
    }

    /**
     * Initialize weights with small random values (Xavier-like scaling),
     * and set biases to zero.
     */
    private void initializeParameters() {
        Random rnd = new Random();
        double std = Math.sqrt(2.0 / (inputSize + outputSize)); // Scale for stable activations
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < outputSize; j++) {
                weights[i][j] = rnd.nextGaussian() * std;
            }
        }
        for (int j = 0; j < outputSize; j++) {
            biases[j] = 0;
        }
    }

    /**
     * Forward pass through the layer.
     * Computes z = Wᵗ * aPrev + b, then a = activation(z)
     */
    @Override
    public double[] forward(double[] aPrev) {
        lastInput = aPrev.clone(); // Cache input for use in backprop

        double[] z = new double[outputSize];
        for (int j = 0; j < outputSize; j++) {
            double sum = biases[j];
            for (int i = 0; i < inputSize; i++) {
                sum += weights[i][j] * aPrev[i];
            }
            z[j] = sum;
        }
        lastZ = z; // Store raw output

        // Apply activation function to each output neuron
        double[] a = new double[outputSize];
        for (int j = 0; j < outputSize; j++) {
            a[j] = act.apply(z[j]);
        }

        return a;
    }

    /**
     * Backward pass: computes gradients for weights, biases, and previous layer.
     * Uses chain rule: dZ = dA * activation'(Z)
     */
    @Override
    public LayerGradients backward(double[] dA) {
        double[] dZ = new double[outputSize];
        for (int j = 0; j < outputSize; j++) {
            dZ[j] = dA[j] * act.derivative(lastZ[j]); // Element-wise derivative
        }

        // Allocate gradient containers
        double[][] dW = new double[inputSize][outputSize];
        double[] dB   = new double[outputSize];
        double[] dAprev = new double[inputSize];

        // Compute gradients
        for (int j = 0; j < outputSize; j++) {
            dB[j] = dZ[j]; // Bias gradient is just dZ

            for (int i = 0; i < inputSize; i++) {
                dW[i][j] = lastInput[i] * dZ[j];            // ∂L/∂W = input * dZ
                dAprev[i] += weights[i][j] * dZ[j];         // ∂L/∂aPrev (for previous layer)
            }
        }

        return new LayerGradients(dW, dB, dAprev);
    }

    // --- Accessors for external use (e.g., training or serialization) ---

    public int getInputSize() {
        return inputSize;
    }

    public int getOutputSize() {
        return outputSize;
    }

    public double[][] getWeights() {
        return weights;
    }

    public double[] getBiases() {
        return biases;
    }

    /**
     * Apply a precomputed gradient update manually (used in simple optimizers).
     */
    public void applyGradients(LayerGradients grads, double learningRate) {
        double[][] dW = grads.dW;
        double[] dB = grads.dB;

        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < outputSize; j++) {
                weights[i][j] -= learningRate * dW[i][j];
            }
        }
        for (int j = 0; j < outputSize; j++) {
            biases[j] -= learningRate * dB[j];
        }
    }
}
