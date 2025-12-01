package main.java.core;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PkccWriter {
    public static void write(OutputStream out, EncodedImage encoded) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);

        Codebook cb = encoded.getCodebook();
        int width  = encoded.getWidth();
        int height = encoded.getHeight();
        int k      = cb.getSize();
        int blockW = cb.getBlockWidth();
        int blockH = cb.getBlockHeight();
        int[] indices = encoded.getBlockIndices();

        // Header
        dos.writeBytes("PKCC");    // magic
        dos.writeShort(Main.VERSION);        // version
        dos.writeInt(width);
        dos.writeInt(height);
        dos.writeByte(blockW);
        dos.writeByte(blockH);
        dos.writeShort(k);

        // Codebook: quantize centroids to bytes
        int vectorLength = blockW * blockH;
        for (int i = 0; i < k; i++) {
            float[] c = cb.getCentroid(i);
            for (int j = 0; j < vectorLength; j++) {
                int gray = Math.round(c[j]);
                if (gray < 0) gray = 0;
                if (gray > 255) gray = 255;
                dos.writeByte(gray);
            }
        }

        // Indices (simple 16-bit each for now)
        for (int idx : indices) {
            if(PkcCompressor.K <= 256) { // If K can be stored in only 1 byte, do so
                dos.writeByte(idx); // writes the low 8 bits
            } else {
                dos.writeShort(idx); // Otherwise, use 2 bytes
            }
        }
    }
}
