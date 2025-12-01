package main.java.io;

import java.io.IOException;
import java.io.InputStream;

public final class BitInputStream {
    private final InputStream in;
    private int currentByte = 0;
    private int numBitsRemaining = 0;

    public BitInputStream(InputStream in) {
        this.in = in;
    }

    // Read a single bit or -1 on EOF
    public int readBit() throws IOException {
        if (numBitsRemaining == 0) {
            currentByte = in.read();
            if (currentByte == -1) {
                return -1;
            }
            numBitsRemaining = 8;
        }
        numBitsRemaining--;
        return (currentByte >>> numBitsRemaining) & 1;
    }
}
