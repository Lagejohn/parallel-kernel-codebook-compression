package main.java.model;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class GrayscaleImage {
    private final int width;
    private final int height;
    private final byte[] pixels; // row-major, 0..255 stored as signed bytes

    public GrayscaleImage(String origFilePath) throws IOException {
        File origFile = new File(origFilePath);
        BufferedImage img = ImageIO.read(origFile);

        // ******** Preprocess input file into raw grayscale byte array ********
        this.width = img.getWidth();
        this.height = img.getHeight();
        this.pixels = new byte[width * height];

        // ensure all pixels are in grayscale
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (r * 299 + g * 587 + b * 114) / 1000; // simple luma
                pixels[y * width + x] = (byte) gray;
            }
        }

        if (pixels.length != width * height) {
            throw new IllegalArgumentException("Pixel buffer length != width*height");
        }
    }

    public GrayscaleImage(int width, int height, byte[] pixels) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }

    // Return 0..255
    public int get(int x, int y) {
        return pixels[y * width + x] & 0xFF;
    }

    // Accept 0..255
    public void set(int x, int y, int gray) {
        pixels[y * width + x] = (byte) gray;
    }

    public byte[] getPixels() {
        return pixels;
    }
}