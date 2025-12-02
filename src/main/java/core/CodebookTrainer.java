package main.java.core;

import main.java.model.Codebook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

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

    public static Codebook trainKMeansParallel(
            List<float[]> vectors,
            int blockWidth,
            int blockHeight,
            int k,
            int maxIterations,
            long rngSeed,
            int numThreads
    ) throws InterruptedException {
        int vectorLength = blockHeight*blockWidth;

        if (vectors == null || vectors.isEmpty()) {
            throw new IllegalArgumentException("No training vectors");
        }
        // Sanity check: all vectors must match vectorLength
        for (float[] v : vectors) {
            if (v.length != vectorLength) {
                throw new IllegalArgumentException("Vector length mismatch: " +
                        v.length + " != " + vectorLength);
            }
        }

        final int n = vectors.size();
        final float[][] data = vectors.toArray(new float[n][]);

        // Thread count: don't exceed number of vectors
        int threads = Math.max(1, Math.min(numThreads, n));
        System.out.printf("trainKMeansParallel: n=%d, k=%d, dim=%d, threads=%d, maxIter=%d%n",
                n, k, vectorLength, threads, maxIterations);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        Random rnd = new Random(rngSeed);

        // --- 1. Initialize centroids by sampling from training vectors ---
        float[][] centroids = new float[k][vectorLength];
        for (int c = 0; c < k; c++) {
            float[] src = data[rnd.nextInt(n)];
            centroids[c] = src.clone();
        }

        // Global assignments (which centroid each vector uses)
        int[] assignments = new int[n];
        // Initialize to -1 (unassigned)
        Arrays.fill(assignments, -1);

        // These arrays will be reused across iterations to avoid reallocating
        List<Future<Boolean>> futures = new ArrayList<>(threads);

        for (int iter = 0; iter < maxIterations; iter++) {
            System.out.println("KMeans iteration " + iter);

            // --- 2. Assignment step (parallel) ---
            futures.clear();

            // Thread-local accumulators: [thread][cluster][dim], [thread][cluster]
            float[][][] partialSums = new float[threads][k][vectorLength];
            int[][] partialCounts   = new int[threads][k];

            for (int t = 0; t < threads; t++) {
                final int threadId = t;
                final int start = (n * t) / threads;
                final int end   = (n * (t + 1)) / threads;

                float[][] finalCentroids = centroids;
                Callable<Boolean> task = () -> {
                    boolean changedLocal = false;
                    float[][] localSums = partialSums[threadId];
                    int[] localCounts   = partialCounts[threadId];

                    for (int i = start; i < end; i++) {
                        float[] v = data[i];

                        // Find nearest centroid
                        int bestIndex = 0;
                        float bestDist = Float.POSITIVE_INFINITY;

                        for (int c = 0; c < k; c++) {
                            float[] centroid = finalCentroids[c];
                            float dist = 0f;
                            for (int d = 0; d < vectorLength; d++) {
                                float diff = v[d] - centroid[d];
                                dist += diff * diff;
                            }
                            if (dist < bestDist) {
                                bestDist = dist;
                                bestIndex = c;
                            }
                        }

                        if (assignments[i] != bestIndex) {
                            assignments[i] = bestIndex;
                            changedLocal = true;
                        }

                        // Accumulate into thread-local sums
                        localCounts[bestIndex]++;
                        float[] sumVec = localSums[bestIndex];
                        for (int d = 0; d < vectorLength; d++) {
                            sumVec[d] += v[d];
                        }
                    }

                    return changedLocal;
                };

                futures.add(pool.submit(task));
            }

            // Combine "changed" flags from threads
            boolean changedAny = false;
            for (Future<Boolean> f : futures) {
                try {
                    if (f.get()) {
                        changedAny = true;
                    }
                } catch (ExecutionException e) {
                    pool.shutdownNow();
                    throw new RuntimeException("KMeans worker failed", e.getCause());
                }
            }

            if (!changedAny && iter > 0) {
                System.out.println("KMeans converged at iteration " + iter);
                break;
            }

            // --- 3. Update step (reduce partial sums) ---

            float[][] newCentroids = new float[k][vectorLength];
            int[] counts = new int[k];

            // Reduce all partialSums/partialCounts into global sums
            for (int t = 0; t < threads; t++) {
                float[][] localSums = partialSums[t];
                int[] localCounts   = partialCounts[t];
                for (int c = 0; c < k; c++) {
                    int cnt = localCounts[c];
                    if (cnt != 0) {
                        counts[c] += cnt;
                        float[] globalSum = newCentroids[c];
                        float[] localSum = localSums[c];
                        for (int d = 0; d < vectorLength; d++) {
                            globalSum[d] += localSum[d];
                        }
                    }
                }
            }

            // Handle empty clusters + compute means
            for (int c = 0; c < k; c++) {
                if (counts[c] == 0) {
                    // Empty cluster: re-seed from a random vector
                    float[] src = data[rnd.nextInt(n)];
                    newCentroids[c] = src.clone();
                } else {
                    float inv = 1.0f / counts[c];
                    float[] centroid = newCentroids[c];
                    for (int d = 0; d < vectorLength; d++) {
                        centroid[d] *= inv;
                    }
                }
            }

            centroids = newCentroids;
        }

        pool.shutdown();

        return new Codebook(blockWidth, blockHeight, centroids);
    }
}
