package main.java.core;

import main.java.model.Codebook;
import main.java.model.EncodedImage;
import main.java.model.GrayscaleImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    public static EncodedImage encodeParallel(GrayscaleImage img, Codebook codebook, int numThreads) throws InterruptedException {
        int width  = img.getWidth();
        int height = img.getHeight();
        int blockW = codebook.getBlockWidth();   // 2
        int blockH = codebook.getBlockHeight();  // 2

        int blocksX = width  / blockW;
        int blocksY = height / blockH;

        int totalBlocks = blocksX * blocksY;
        int[] indices = new int[totalBlocks];

        System.out.printf("encodeParallel: %dx%d blocks (%dx%d), blocksX=%d blocksY=%d totalBlocks=%d%n",
                width, height, blockW, blockH, blocksX, blocksY, totalBlocks);

        if (blocksY == 0 || blocksX == 0) {
            System.out.println("encodeParallel: no blocks to encode");
            return new EncodedImage(width, height, blockW, blockH, codebook, indices);
        }

        int threads = Math.max(1, Math.min(numThreads, blocksY));
        System.out.printf("encodeParallel: initializing %d threads\n", threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        // Partition by *block rows*
        for (int t = 0; t < threads; t++) {
            final int byStart = (blocksY * t) / threads;
            final int byEnd   = (blocksY * (t + 1)) / threads;

            Runnable task = () -> {
                float[] v = new float[blockW * blockH]; // thread-local temp
                for (int by = byStart; by < byEnd; by++) {
                    int y0 = by * blockH;
                    for (int bx = 0; bx < blocksX; bx++) {
                        int x0 = bx * blockW;

                        // Build vector for this block
                        int pos = 0;
                        for (int dy = 0; dy < blockH; dy++) {
                            for (int dx = 0; dx < blockW; dx++) {
                                v[pos++] = img.get(x0 + dx, y0 + dy);
                            }
                        }

                        int codeIdx = codebook.findNearest(v);

                        int blockIndex = by * blocksX + bx; // row-major over blocks
                        indices[blockIndex] = codeIdx;
                    }
                }
            };

            futures.add(pool.submit(task));
        }

        // Wait for all tasks
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (ExecutionException e) {
                pool.shutdownNow();
                throw new RuntimeException("encodeParallel worker failed", e.getCause());
            }
        }

        pool.shutdown();

        return new EncodedImage(width, height, blockW, blockH, codebook, indices);
    }

    public static GrayscaleImage decodeParallel(EncodedImage encoded, int numThreads) throws InterruptedException {
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

        System.out.printf("decodeParallel: %dx%d blocks (%dx%d), blocksX=%d blocksY=%d totalBlocks=%d%n",
                width, height, blockW, blockH, blocksX, blocksY, indices.length);

        if (blocksY == 0 || blocksX == 0) {
            System.out.println("decodeParallel: no blocks to decode");
            return img;
        }

        int threads = Math.max(1, Math.min(numThreads, blocksY));
        System.out.printf("decodeParallel: initializing %d thread\n", threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int byStart = (blocksY * t) / threads;
            final int byEnd   = (blocksY * (t + 1)) / threads;

            Runnable task = () -> {
                for (int by = byStart; by < byEnd; by++) {
                    int y0 = by * blockH;
                    for (int bx = 0; bx < blocksX; bx++) {
                        int blockIndex = by * blocksX + bx;
                        int codeIdx = indices[blockIndex];
                        float[] c = codebook.getCentroid(codeIdx);

                        int pos = 0;
                        for (int dy = 0; dy < blockH; dy++) {
                            int yy = y0 + dy;
                            for (int dx = 0; dx < blockW; dx++) {
                                int xx = bx * blockW + dx;
                                int gray = Math.round(c[pos++]);
                                if (gray < 0) gray = 0;
                                if (gray > 255) gray = 255;
                                img.set(xx, yy, gray);
                            }
                        }
                    }
                }
            };

            futures.add(pool.submit(task));
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (ExecutionException e) {
                pool.shutdownNow();
                throw new RuntimeException("decodeParallel worker failed", e.getCause());
            }
        }

        pool.shutdown();
        return img;
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
