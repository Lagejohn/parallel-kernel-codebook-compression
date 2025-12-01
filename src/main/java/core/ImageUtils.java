package main.java.core;

import main.java.model.GrayscaleImage;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;

public final class ImageUtils {

    private ImageUtils() {
        // utility class, no instances
    }

    /** Convert any BufferedImage to our GrayscaleImage wrapper (8-bit, 0..255). */
    public static GrayscaleImage toGrayscale(BufferedImage src) {
        int width  = src.getWidth();
        int height = src.getHeight();
        byte[] pixels = new byte[width * height];

        // Fast path: already BYTE_GRAY
        if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            WritableRaster raster = src.getRaster();
            DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();
            byte[] data = buffer.getData();

            // Copy so we don't alias the original image's backing array
            System.arraycopy(data, 0, pixels, 0, pixels.length);
        } else {
            // General path: convert from RGB (or whatever) to grayscale
            int idx = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = src.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8)  & 0xFF;
                    int b =  rgb        & 0xFF;

                    // Luma approximation (Rec. 601 coefficients)
                    int gray = (r * 299 + g * 587 + b * 114 + 500) / 1000;
                    if (gray < 0)   gray = 0;
                    if (gray > 255) gray = 255;

                    pixels[idx++] = (byte) gray;
                }
            }
        }

        return new GrayscaleImage(width, height, pixels);
    }

    /** Convert our GrayscaleImage back into a TYPE_BYTE_GRAY BufferedImage. */
    public static BufferedImage fromGrayscale(GrayscaleImage gray) {
        int width  = gray.getWidth();
        int height = gray.getHeight();

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = img.getRaster();
        DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();
        byte[] dest = buffer.getData();

        byte[] srcPixels = gray.getPixels();
        System.arraycopy(srcPixels, 0, dest, 0, srcPixels.length);

        return img;
    }
}
