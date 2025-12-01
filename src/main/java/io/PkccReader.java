package main.java.io;

import main.java.core.HuffmanCodec;
import main.java.core.HuffmanNode;
import main.java.model.Codebook;
import main.java.model.EncodedImage;
import main.java.app.Main;
import main.java.app.PkcCompressor;

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

        // --- Version 2: Huffman-coded indices ---

        // 1) Read code lengths
        int[] codeLen = new int[k];
        for (int s = 0; s < k; s++) {
            codeLen[s] = dis.readUnsignedByte(); // 0..255
        }

        // 2) Rebuild canonical codes
        int[] codeBits = new int[k];
        HuffmanCodec.buildCanonicalCodes(codeLen, codeBits);

        // 3) Build decoding tree
        HuffmanNode decodeRoot = HuffmanCodec.buildDecodingTree(codeLen, codeBits);

        // 4) Decode exactly totalBlocks symbols from bitstream
        int[] indices = new int[totalBlocks];
        BitInputStream bin = new BitInputStream(dis);
        for (int i = 0; i < totalBlocks; i++) {
            int sym = HuffmanCodec.decodeSymbol(bin, decodeRoot);
            indices[i] = sym;
        }

        return new EncodedImage(width, height, blockW, blockH, cb, indices);
    }
}
