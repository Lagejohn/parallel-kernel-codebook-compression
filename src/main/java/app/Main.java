package main.java.core;

import java.io.IOException;

public class Main {
    public static final byte VERSION = 0x0001;

    public static void main(String[] args) throws IOException {
        System.out.printf("Parallel-Kernel Codebook Compression Codec v%s\n\n", VERSION);
        PkcCompressor compressor = new PkcCompressor(args[0]);

        if(args[0].endsWith(".pkcc")) {
            System.out.printf("Decompressing image file %s", args[0]);

            try {
                compressor.decompress();
            } catch (Exception e) {
                throw new RuntimeException("Failed to decompress image.", e);
            }

        } else {
            System.out.printf("Compressing image file %s", args[0]);
            try {
                compressor.compress();
            } catch (Exception e) {
                throw new RuntimeException("Failed to compress image.", e);
            }
        }
    }
}