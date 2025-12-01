package main.java.core;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PkccReader {
    public static EncodedImage read(InputStream in) throws IOException {
        DataInputStream dis = new DataInputStream(in);

        byte[] magic = new byte[4];
        dis.readFully(magic);
        if (!new String(magic, StandardCharsets.US_ASCII).equals("PKCC")) {
            throw new IOException("Bad magic");
        }

        int version = dis.readUnsignedShort();
        if(version != Main.VERSION) {
            System.out.printf("WARNING: Codec version %s does not match file version %s", Main.VERSION, version);
        }

        int width  = dis.readInt();
        int height = dis.readInt();
        int blockW = dis.readUnsignedByte();
        int blockH = dis.readUnsignedByte();
        int k      = dis.readUnsignedShort();

        int vectorLength = blockW * blockH;
        float[][] centroids = new float[k][vectorLength];

        for (int i = 0; i < k; i++) {
            for (int j = 0; j < vectorLength; j++) {
                int gray = dis.readUnsignedByte();
                centroids[i][j] = gray;
            }
        }

        Codebook cb = new Codebook(blockW, blockH, centroids);

        int blocksX = width  / blockW;
        int blocksY = height / blockH;
        int totalBlocks = blocksX * blocksY;
        int[] indices = new int[totalBlocks];

        for (int i = 0; i < totalBlocks; i++) {
            if(PkcCompressor.K <= 256) {
                indices[i] = dis.readUnsignedByte(); // If K can be stored in only 1 byte, do so
            } else {
                indices[i] = dis.readUnsignedShort(); // Otherwise, use 2 bytes
            }

        }

        return new EncodedImage(width, height, blockW, blockH, cb, indices);
    }
}
