package main.java.core;

import java.util.List;
import java.util.Random;

public final class CodebookTrainer {

    public static Codebook trainKMeans(
            List<float[]> vectors,
            int width,
            int height,
            int k,
            int maxIterations,
            long rngSeed
    ) {
        int vectorLength = width*height;

        if (vectors.isEmpty()) {
            throw new IllegalArgumentException("No training vectors");
        }
        Random rnd = new Random(rngSeed);

        // 1. Initialize centroids randomly from existing vectors
        float[][] centroids = new float[k][vectorLength];
        for (int i = 0; i < k; i++) {
            float[] src = vectors.get(rnd.nextInt(vectors.size()));
            centroids[i] = src.clone();
        }

        int n = vectors.size();
        int[] assignments = new int[n]; // which centroid each vector uses

        for (int iter = 0; iter < maxIterations; iter++) {
            boolean changed = false;

            // 2. Assignment step
            for (int i = 0; i < n; i++) {
                float[] v = vectors.get(i);
                int bestIndex = 0;
                float bestDist = Float.POSITIVE_INFINITY;

                for (int c = 0; c < k; c++) {
                    float dist = 0f;
                    float[] centroid = centroids[c];
                    for (int j = 0; j < vectorLength; j++) {
                        float d = v[j] - centroid[j];
                        dist += d * d;
                    }
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestIndex = c;
                    }
                }

                if (assignments[i] != bestIndex) {
                    assignments[i] = bestIndex;
                    changed = true;
                }
            }

            if (!changed) {
                // Converged
                break;
            }

            // 3. Update step
            float[][] newCentroids = new float[k][vectorLength];
            int[] counts = new int[k];

            for (int i = 0; i < n; i++) {
                int c = assignments[i];
                float[] v = vectors.get(i);
                counts[c]++;
                float[] acc = newCentroids[c];
                for (int j = 0; j < vectorLength; j++) {
                    acc[j] += v[j];
                }
            }

            // Avoid empty clusters: re-seed them randomly
            for (int c = 0; c < k; c++) {
                if (counts[c] == 0) {
                    float[] src = vectors.get(rnd.nextInt(vectors.size()));
                    newCentroids[c] = src.clone();
                } else {
                    float inv = 1.0f / counts[c];
                    for (int j = 0; j < vectorLength; j++) {
                        newCentroids[c][j] *= inv;
                    }
                }
            }

            centroids = newCentroids;
        }

        return new Codebook(width, height, centroids);
    }
}
