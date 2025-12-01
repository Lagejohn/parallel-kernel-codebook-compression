package main.java.core;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class PkcCompressor {
    public static final int KERNEL_WIDTH = 1;
    public static final int KERNEL_HEIGHT = 3;
    public static final int K = 256;
    public static final float SAMPLE_RATE = 0.25F;
    public static final int MAX_ITER = 10;
    public static final String OUTPUT_FORMAT = "png";

    private final String originalFilePath;

    public PkcCompressor(String origFilePath) throws IOException {
        this.originalFilePath = origFilePath;
    }


    public void compress() throws IOException {
        if (K > 256) {
            throw new IllegalArgumentException("This codec currently supports K <= 256 (one-byte indices).");
        }

        GrayscaleImage image = new GrayscaleImage(originalFilePath);
        System.out.println("Input image: " + image.getWidth() + "x" + image.getHeight());

        // 2) Collect training vectors (overlapping 2x2 with subsampling)

        List<float[]> training = TrainingVectorCollector.collectTrainingVectors(image, KERNEL_WIDTH, KERNEL_HEIGHT, SAMPLE_RATE, 1234L);
        System.out.println("Training vectors: " + training.size());

        // 3) Train codebook
        Codebook cb = CodebookTrainer.trainKMeans(training, KERNEL_WIDTH, KERNEL_HEIGHT, K, MAX_ITER, 1234L);
        System.out.println("Codebook size: " + cb.getSize());

        // 4) Encode image with codebook
        EncodedImage encoded = BlockEncoder.encode(image, cb);
        System.out.println("Encoded blocks: " + encoded.getBlockIndices().length);

        // 5) Write to file (no Huffman yet)
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(Path.of(originalFilePath.split("\\.")[0] + "-compressed.pkcc")))) {
            System.out.println("About to write .pkcc file...");
            PkccWriter.write(out, encoded);
            System.out.println("Finished writing .pkcc file.");
        }

    }

    public void decompress() {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(Path.of(originalFilePath)))) {
            System.out.println("Opened InputStream for MVQ file");

            // 2) Read encoded image structure
            EncodedImage encoded = PkccReader.read(in);
            System.out.println("MvqReader.read() returned EncodedImage");

            System.out.printf("EncodedImage: %dx%d, block=%dx%d, codebookSize=%d, blocks=%d%n",
                    encoded.getWidth(),
                    encoded.getHeight(),
                    encoded.getBlockWidth(),
                    encoded.getBlockHeight(),
                    encoded.getCodebook().getSize(),
                    encoded.getBlockIndices().length);

            // 3) Decode indices → GrayscaleImage
            GrayscaleImage gray = BlockEncoder.decode(encoded);
            System.out.println("BlockEncoder.decode() produced GrayscaleImage: "
                    + gray.getWidth() + "x" + gray.getHeight());

            // 4) Convert GrayscaleImage → BufferedImage
            BufferedImage output = ImageUtils.fromGrayscale(gray);
            System.out.println("Converted GrayscaleImage to BufferedImage");

            // 5) Write to desired image format
            Path outputImagePath = Path.of(originalFilePath.split("\\.pkcc")[0]+"-recon."+OUTPUT_FORMAT);
            boolean ok = ImageIO.write(output, OUTPUT_FORMAT, outputImagePath.toFile());
            System.out.println("ImageIO.write returned: " + ok);

            if (!ok) {
                throw new IOException("No appropriate writer found for format: " + OUTPUT_FORMAT);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
