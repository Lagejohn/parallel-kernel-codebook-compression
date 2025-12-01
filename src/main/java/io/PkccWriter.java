package main.java.io;

import main.java.core.HuffmanCodec;
import main.java.model.Codebook;
import main.java.model.EncodedImage;
import main.java.app.Main;
import main.java.app.PkcCompressor;

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

        // 1) Build frequency table
        int[] freq = new int[k];
        for (int idx : indices) {
            if (idx < 0 || idx >= k) {
                throw new IOException("Index out of range: " + idx);
            }
            freq[idx]++;
        }

        // 2) Build code lengths
        int[] codeLen = HuffmanCodec.buildCodeLengths(freq);

        // 3) Build canonical codes
        int[] codeBits = new int[k];
        HuffmanCodec.buildCanonicalCodes(codeLen, codeBits);

        // 4) Write code lengths (K bytes, zero = unused symbol)
        System.out.println("Writer: writing Huffman code lengths for " + k + " symbols");
        for (int s = 0; s < k; s++) {
            int len = codeLen[s];
            if (len > 255) {
                throw new IOException("Code length too large: " + len);
            }
            dos.writeByte(len); // 0..255
        }

        // 5) Write Huffman-coded indices as bitstream
        System.out.println("Writer: writing Huffman-coded indices: " + indices.length + " symbols");
        try (BitOutputStream bout = new BitOutputStream(dos)) {
            for (int idx : indices) {
                int len = codeLen[idx];
                int code = codeBits[idx];
                bout.writeBits(code, len);
            }
        }
    }
}
