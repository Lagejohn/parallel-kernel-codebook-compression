package main.java.app;

import main.java.core.BlockEncoder;
import main.java.core.CodebookTrainer;
import main.java.core.ImageUtils;
import main.java.core.TrainingVectorCollector;
import main.java.model.*;
import main.java.io.PkccReader;
import main.java.io.PkccWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class PkcCompressor {
    public static final int KERNEL_WIDTH = 2;
    public static final int KERNEL_HEIGHT = 2;
    public static final int K = 256;
    public static final float SAMPLE_RATE = 0.25F;
    public static final int MAX_ITER = 10;
    public static final String OUTPUT_FORMAT = "png";

    private final String originalFilePath;

    public PkcCompressor(String origFilePath) throws IOException {
        this.originalFilePath = origFilePath;
    }


    public void compress(boolean multithreading) throws IOException, InterruptedException {
        System.out.println("Using multithreading: "+multithreading);
        int cores = Runtime.getRuntime().availableProcessors();
        if (K > 256) {
            throw new IllegalArgumentException("This codec currently supports K <= 256 (one-byte indices).");
        }

        // 1) Process image into grayscale format
        GrayscaleImage image = new GrayscaleImage(originalFilePath);
        System.out.println("Input image: " + image.getWidth() + "x" + image.getHeight());

        // 2) Collect training vectors (overlapping 2x2 with subsampling)

        List<float[]> training = TrainingVectorCollector.collectTrainingVectors(image, KERNEL_WIDTH, KERNEL_HEIGHT, SAMPLE_RATE, 1234L);
        System.out.println("Training vectors: " + training.size());

        // 3) Train codebook
        Codebook cb;
        if(multithreading) {
            cb = CodebookTrainer.trainKMeansParallel(training, KERNEL_WIDTH, KERNEL_HEIGHT, K, MAX_ITER, 1234L, cores);
        } else {
            cb = CodebookTrainer.trainKMeans(training, KERNEL_WIDTH, KERNEL_HEIGHT, K, MAX_ITER, 1234L);
        }
        System.out.println("Codebook size: " + cb.getSize());

        // 4) Encode image with codebook
        EncodedImage encoded;
        if(multithreading) {
            encoded = BlockEncoder.encodeParallel(image, cb, cores);
        } else {
            encoded = BlockEncoder.encode(image, cb);
        }
        System.out.println("Encoded blocks: " + encoded.getBlockIndices().length);

        // 5) Write to file
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(Path.of(originalFilePath.split("\\.")[0] + "-compressed.pkcc")))) {
            System.out.println("About to write .pkcc file...");
            PkccWriter.write(out, encoded);
            System.out.println("Finished writing .pkcc file.");
        }

    }

    public void decompress(boolean multithreading) throws InterruptedException {
        System.out.println("Using multithreading: "+multithreading);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(Path.of(originalFilePath)))) {
            System.out.println("Opened InputStream for PKCC file");

            // 2) Read encoded image structure
            EncodedImage encoded = PkccReader.read(in);
            System.out.println("PkccReader.read() returned EncodedImage");

            System.out.printf("EncodedImage: %dx%d, block=%dx%d, codebookSize=%d, blocks=%d%n",
                    encoded.getWidth(),
                    encoded.getHeight(),
                    encoded.getBlockWidth(),
                    encoded.getBlockHeight(),
                    encoded.getCodebook().getSize(),
                    encoded.getBlockIndices().length);

            // 3) Decode indices → GrayscaleImage
            GrayscaleImage gray;
            if(multithreading) {
                int cores = Runtime.getRuntime().availableProcessors();
                gray = BlockEncoder.decodeParallel(encoded, cores);
                System.out.println("BlockEncoder.decodeParallel() produced GrayscaleImage: "
                        + gray.getWidth() + "x" + gray.getHeight());
            } else {
                gray = BlockEncoder.decode(encoded);
                System.out.println("BlockEncoder.decode() produced GrayscaleImage: "
                        + gray.getWidth() + "x" + gray.getHeight());
            }

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
