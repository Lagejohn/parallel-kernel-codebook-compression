package main.java.core;

import main.java.model.GrayscaleImage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class TrainingVectorCollector {

    /**
     * Collect 1D block vectors of length blockWidth from the image.
     *
     *   - strideX = 1 (overlapping) by default
     *   - sampleRate in (0,1]: probability of keeping a given block (for subsampling)
     *
     * Returns a list of float[] where each float[] has length blockWidth.
     */
    public static List<float[]> collectTrainingVectors(
            GrayscaleImage img,
            int blockWidth,
            int blockHeight,
            double sampleRate,
            long rngSeed
    ) {
        if (blockWidth <= 0 || blockHeight <= 0) {
            throw new IllegalArgumentException("blockWidth and blockHeight must be > 0");
        }
        if (sampleRate <= 0.0 || sampleRate > 1.0) {
            throw new IllegalArgumentException("sampleRate must be in (0,1]");
        }

        int width  = img.getWidth();
        int height = img.getHeight();

        int vectorLength = blockWidth * blockHeight;
        Random rnd = new Random(rngSeed);
        List<float[]> vectors = new ArrayList<>();

        // Overlapping blocks: slide by 1 pixel in both directions
        for (int y = 0; y <= height - blockHeight; y++) {
            for (int x = 0; x <= width - blockWidth; x++) {

                // Random subsample if desired
                if (rnd.nextDouble() > sampleRate) {
                    continue;
                }

                float[] v = new float[vectorLength];
                int idx = 0;

                // Fill vector row-major
                for (int dy = 0; dy < blockHeight; dy++) {
                    for (int dx = 0; dx < blockWidth; dx++) {
                        v[idx++] = img.get(x + dx, y + dy);
                    }
                }

                vectors.add(v);
            }
        }

        return vectors;
    }
}
