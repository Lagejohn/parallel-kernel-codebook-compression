package main.java.io;

import java.io.IOException;
import java.io.OutputStream;

public final class BitOutputStream implements AutoCloseable {
    private final OutputStream out;
    private int currentByte = 0;
    private int numBitsFilled = 0;

    public BitOutputStream(OutputStream out) {
        this.out = out;
    }

    // Write 'numBits' lowest bits of 'code' (MSB-first in the bitstream)
    public void writeBits(int code, int numBits) throws IOException {
        for (int i = numBits - 1; i >= 0; i--) {
            int bit = (code >>> i) & 1;
            currentByte = (currentByte << 1) | bit;
            numBitsFilled++;
            if (numBitsFilled == 8) {
                out.write(currentByte);
                numBitsFilled = 0;
                currentByte = 0;
            }
        }
    }

    public void flush() throws IOException {
        if (numBitsFilled > 0) {
            currentByte <<= (8 - numBitsFilled); // pad with zeros
            out.write(currentByte);
            currentByte = 0;
            numBitsFilled = 0;
        }
        out.flush();
    }

    @Override
    public void close() throws IOException {
        flush();
        out.close();
    }
}
