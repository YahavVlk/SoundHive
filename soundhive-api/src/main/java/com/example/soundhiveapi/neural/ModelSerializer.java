package com.example.soundhiveapi.neural;

import java.io.*;
import java.util.List;

/**
 * Save and load a NeuralNetwork's weights & biases in a simple binary format.
 * This is a custom serializer that persists all layers' parameters to disk.
 */
public class ModelSerializer {
    private static final String MODEL_FILE = "model.dat"; // Default file name

    /**
     * Save the entire model to disk (weights and biases of each layer).
     * Format:
     * - number of layers
     * - for each layer: input size, output size, weights (row-major), then biases
     */
    public static void saveModel(NeuralNetwork net) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(MODEL_FILE)))) {

            List<Layer> layers = net.getLayers();
            out.writeInt(layers.size()); // total number of layers

            for (Layer lay : layers) {
                DenseLayer d = (DenseLayer) lay;
                int inSize  = d.getInputSize();
                int outSize = d.getOutputSize();

                out.writeInt(inSize);  // write input size
                out.writeInt(outSize); // write output size

                // Write weight matrix in row-major order
                double[][] W = d.getWeights();
                for (int i = 0; i < inSize; i++) {
                    for (int j = 0; j < outSize; j++) {
                        out.writeDouble(W[i][j]);
                    }
                }

                // Write bias vector
                double[] B = d.getBiases();
                for (int j = 0; j < outSize; j++) {
                    out.writeDouble(B[j]);
                }
            }
        }
    }

    /**
     * Load a model from disk by reading layer sizes, weights, and biases from MODEL_FILE.
     * Fills the layers of the provided NeuralNetwork instance.
     */
    public static void loadModel(NeuralNetwork net) throws IOException {
        File f = new File(MODEL_FILE);
        if (!f.exists()) return;  // If the file doesn't exist, skip loading

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(f)))) {

            int numLayers = in.readInt(); // number of layers to read
            List<Layer> layers = net.getLayers();

            for (int l = 0; l < numLayers; l++) {
                DenseLayer d = (DenseLayer) layers.get(l);

                int inSize  = in.readInt();
                int outSize = in.readInt();

                // Load weights
                double[][] W = d.getWeights();
                for (int i = 0; i < inSize; i++) {
                    for (int j = 0; j < outSize; j++) {
                        W[i][j] = in.readDouble();
                    }
                }

                // Load biases
                double[] B = d.getBiases();
                for (int j = 0; j < outSize; j++) {
                    B[j] = in.readDouble();
                }
            }
        }
    }
}
