package main.java.model;

public final class Codebook {
    private final int width, height;
    private final int vectorLength; // e.g. 3 for 1x3, 4 for 2x2
    private final int size;          // number of codewords
    private final float[][] centroids; // [size][blockWidth]

    public Codebook(int width, int height, float[][] centroids) {
        this.width = width;
        this.height = height;
        this.vectorLength = width*height;
        this.size = centroids.length;
        this.centroids = centroids;
    }

    public int getVectorLength() {
        return vectorLength;
    }

    public int getSize() {
        return size;
    }

    // Centroid values may be floats from training; later you'll quantize to bytes for storage.
    public float[] getCentroid(int i) {
        return centroids[i];
    }

    /**
     * Brute-force nearest neighbor search.
     * Returns index of centroid with minimal squared distance.
     */
    public int findNearest(float[] vector) {
        int bestIndex = 0;
        float bestDist = Float.POSITIVE_INFINITY;

        for (int i = 0; i < size; i++) {
            float[] c = centroids[i];
            float dist = 0f;
            for (int j = 0; j < vectorLength; j++) {
                float d = vector[j] - c[j];
                dist += d * d;
            }
            if (dist < bestDist) {
                bestDist = dist;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    public int getBlockWidth() {
        return width;
    }

    public int getBlockHeight() {
        return height;
    }
}
