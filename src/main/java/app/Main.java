package main.java.app;

import java.io.IOException;
import java.lang.reflect.Array;

public class Main {
    public static final byte VERSION = 0x0002;

    public static void main(String[] args) throws IOException, InterruptedException {
        long start = System.nanoTime();
        System.out.printf("Parallel-Kernel Codebook Compression Codec v%s\n\n", VERSION);
        PkcCompressor compressor = new PkcCompressor(args[0]);

        if(args[0].endsWith(".pkcc")) {
            System.out.printf("Decompressing image file %s\n", args[0]);
            try {
                if(args[1].equalsIgnoreCase("-m") || args[1].equalsIgnoreCase("--multithread")) {
                    compressor.decompress(true);
                } else {
                    compressor.decompress(false);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                compressor.decompress(false);
            } catch (Exception e) {
                throw new RuntimeException("Failed to decompress image.", e);
            }

        } else {
            System.out.printf("Compressing image file %s\n", args[0]);
            try {
                if(args[1].equalsIgnoreCase("-m") || args[1].equalsIgnoreCase("--multithread")) {
                    compressor.compress(true);
                } else {
                    compressor.compress(false);
                }

            } catch (ArrayIndexOutOfBoundsException e) {
                compressor.compress(false);
            } catch (Exception e) {
                throw new RuntimeException("Failed to compress image.", e);
            }
        }
        long end = System.nanoTime();
        double millis = (end - start) / 1000000.0;
        System.out.println("End-to-end execution time in milliseconds: "+millis);
    }
}