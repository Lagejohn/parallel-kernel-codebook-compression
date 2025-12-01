package main.java.core;

import main.java.model.Codebook;
import main.java.model.EncodedImage;
import main.java.model.GrayscaleImage;

import java.io.IOException;

public final class BlockEncoder {

    public static EncodedImage encode(GrayscaleImage img, Codebook codebook) {
        int width  = img.getWidth();
        int height = img.getHeight();
        int blockW = codebook.getBlockWidth();
        int blockH = codebook.getBlockHeight();

        int blocksX = width  / blockW;
        int blocksY = height / blockH;
        int totalBlocks = blocksX * blocksY;

        int[] indices = new int[totalBlocks];

        float[] v = new float[blockW * blockH];

        int idx = 0;
        for (int by = 0; by < blocksY; by++) {
            int y0 = by * blockH;
            for (int bx = 0; bx < blocksX; bx++) {
                int x0 = bx * blockW;

                int t = 0;
                for (int dy = 0; dy < blockH; dy++) {
                    for (int dx = 0; dx < blockW; dx++) {
                        v[t++] = img.get(x0 + dx, y0 + dy);
                    }
                }

                int codeIdx = codebook.findNearest(v);
                indices[idx++] = codeIdx;
            }
        }

        // TODO: handle leftover right/bottom edges (for now will assert width%2==0 and height%2==0)

        return new EncodedImage(width, height, blockW, blockH, codebook, indices);
    }

    public static GrayscaleImage decode(EncodedImage encoded) throws IOException {
        int width  = encoded.getWidth();
        int height = encoded.getHeight();

        int blockW = encoded.getBlockWidth();
        int blockH = encoded.getBlockHeight();
        int[] indices = encoded.getBlockIndices();
        Codebook codebook = encoded.getCodebook();

        int blocksX = width  / blockW;
        int blocksY = height / blockH;

        byte[] pixels = new byte[width * height];
        GrayscaleImage img = new GrayscaleImage(width, height, pixels);

        int idx = 0;
        for (int by = 0; by < blocksY; by++) {
            int y0 = by * blockH;
            for (int bx = 0; bx < blocksX; bx++) {
                int codeIdx = indices[idx++];
                float[] c = codebook.getCentroid(codeIdx);

                int t = 0;
                for (int dy = 0; dy < blockH; dy++) {
                    for (int dx = 0; dx < blockW; dx++) {
                        int gray = Math.round(c[t++]);
                        if (gray < 0) gray = 0;
                        if (gray > 255) gray = 255;
                        img.set(bx * blockW + dx, by * blockH + dy, gray);
                    }
                }
            }
        }

        return img;
    }
}
