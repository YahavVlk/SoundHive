package com.example.soundhiveapi.neural;

import java.util.Random;

/**
 * Fully‑connected (dense) layer implementation.
 */
public class DenseLayer implements Layer {
    private final int inputSize, outputSize;
    private final ActivationFunction act;
    private final double[][] weights;
    private final double[]   biases;

    // Stored during forward pass for use in backprop
    private double[] lastInput;
    private double[] lastZ;

    public DenseLayer(int inputSize, int outputSize, ActivationFunction act) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.act = act;
        this.weights = new double[inputSize][outputSize];
        this.biases  = new double[outputSize];
        initializeParameters();
    }

    /** Initialize weights with small random values and biases to zero. */
    private void initializeParameters() {
        Random rnd = new Random();
        double std = Math.sqrt(2.0 / (inputSize + outputSize));
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < outputSize; j++) {
                weights[i][j] = rnd.nextGaussian() * std;
            }
        }
        for (int j = 0; j < outputSize; j++) {
            biases[j] = 0;
        }
    }

    @Override
    public double[] forward(double[] aPrev) {
        // cache input for backprop
        lastInput = aPrev.clone();
        // compute z = W^T * aPrev + b
        double[] z = new double[outputSize];
        for (int j = 0; j < outputSize; j++) {
            double sum = biases[j];
            for (int i = 0; i < inputSize; i++) {
                sum += weights[i][j] * aPrev[i];
            }
            z[j] = sum;
        }
        lastZ = z;
        // apply activation
        double[] a = new double[outputSize];
        for (int j = 0; j < outputSize; j++) {
            a[j] = act.apply(z[j]);
        }
        return a;
    }

    @Override
    public LayerGradients backward(double[] dA) {
        // dZ = dA * activation'(z)
        double[] dZ = new double[outputSize];
        for (int j = 0; j < outputSize; j++) {
            dZ[j] = dA[j] * act.derivative(lastZ[j]);
        }

        // allocate gradient containers
        double[][] dW = new double[inputSize][outputSize];
        double[] dB   = new double[outputSize];
        double[] dAprev = new double[inputSize];

        // compute gradients
        for (int j = 0; j < outputSize; j++) {
            dB[j] = dZ[j];
            for (int i = 0; i < inputSize; i++) {
                dW[i][j] = lastInput[i] * dZ[j];
                dAprev[i] += weights[i][j] * dZ[j];
            }
        }

        return new LayerGradients(dW, dB, dAprev);
    }

    public int getInputSize() {
        return inputSize;
    }
    public int getOutputSize() {
        return outputSize;
    }
    public double[][] getWeights() {
        return weights;
    }
    public double[]   getBiases() {
        return biases;
    }

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
