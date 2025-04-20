package com.example.soundhiveapi.neural;

import java.io.*;
import java.util.List;

/**
 * Save and load a NeuralNetwork's weights & biases in a simple binary format.
 */
public class ModelSerializer {
    private static final String MODEL_FILE = "model.dat";

    /** Write all layers' weights and biases to MODEL_FILE. */
    public static void saveModel(NeuralNetwork net) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(MODEL_FILE)))) {
            List<Layer> layers = net.getLayers();
            out.writeInt(layers.size());
            for (Layer lay : layers) {
                DenseLayer d = (DenseLayer) lay;
                int inSize  = d.getInputSize();
                int outSize = d.getOutputSize();
                out.writeInt(inSize);
                out.writeInt(outSize);
                // write weights row‑major
                double[][] W = d.getWeights();
                for (int i = 0; i < inSize; i++) {
                    for (int j = 0; j < outSize; j++) {
                        out.writeDouble(W[i][j]);
                    }
                }
                // write biases
                double[] B = d.getBiases();
                for (int j = 0; j < outSize; j++) {
                    out.writeDouble(B[j]);
                }
            }
        }
    }

    /** Read MODEL_FILE and load all weights & biases back into net. */
    public static void loadModel(NeuralNetwork net) throws IOException {
        File f = new File(MODEL_FILE);
        if (!f.exists()) return;  // nothing to load
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(f)))) {
            int numLayers = in.readInt();
            List<Layer> layers = net.getLayers();
            for (int l = 0; l < numLayers; l++) {
                DenseLayer d = (DenseLayer) layers.get(l);
                int inSize  = in.readInt();
                int outSize = in.readInt();
                double[][] W = d.getWeights();
                for (int i = 0; i < inSize; i++) {
                    for (int j = 0; j < outSize; j++) {
                        W[i][j] = in.readDouble();
                    }
                }
                double[] B = d.getBiases();
                for (int j = 0; j < outSize; j++) {
                    B[j] = in.readDouble();
                }
            }
        }
    }
}
