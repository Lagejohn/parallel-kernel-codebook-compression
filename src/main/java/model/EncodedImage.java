package main.java.core;

public final class EncodedImage {
    private final int width, height;
    private final int blockWidth, blockHeight;
    private final Codebook codebook;
    private final int[] blockIndices;

    public EncodedImage(int width, int height, int blockWidth, int blockHeight, Codebook codebook, int[] blockIndices) {
        this.width = width;
        this.height = height;
        this.blockWidth = blockWidth;
        this.blockHeight = blockHeight;
        this.codebook = codebook;
        this.blockIndices = blockIndices;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBlockWidth() {
        return blockWidth;
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public Codebook getCodebook() {
        return codebook;
    }

    public int[] getBlockIndices() {
        return blockIndices;
    }

    // ctor + getters...
}
