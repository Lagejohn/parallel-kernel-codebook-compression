package main.java.core;

import main.java.io.BitInputStream;

import java.util.*;

public final class HuffmanCodec {

    // Build code lengths for symbols 0..(freq.length-1)
    public static int[] buildCodeLengths(int[] freq) {
        int K = freq.length;

        // Special case: if only one symbol ever appears, give it length 1
        int nonZeroCount = 0;
        for (int f : freq) {
            if (f > 0) nonZeroCount++;
        }
        if (nonZeroCount == 0) {
            throw new IllegalArgumentException("All frequencies are zero");
        }

        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
        for (int s = 0; s < K; s++) {
            if (freq[s] > 0) {
                pq.add(new HuffmanNode(s, freq[s], null, null));
            }
        }
        if (pq.size() == 1) {
            // Only one symbol: add a dummy zero-frequency node so tree has depth 1
            pq.add(new HuffmanNode(-1, 0, null, null));
        }

        // Build tree
        while (pq.size() > 1) {
            HuffmanNode n1 = pq.poll();
            HuffmanNode n2 = pq.poll();
            HuffmanNode parent = new HuffmanNode(-1, n1.freq + n2.freq, n1, n2);
            pq.add(parent);
        }
        HuffmanNode root = pq.poll();

        // Traverse to get code lengths
        int[] codeLen = new int[K];
        fillCodeLengths(root, 0, codeLen);
        return codeLen;
    }

    private static void fillCodeLengths(HuffmanNode node, int depth, int[] codeLen) {
        if (node.isLeaf()) {
            if (node.symbol >= 0) {
                codeLen[node.symbol] = depth == 0 ? 1 : depth; // at least 1 bit
            }
        } else {
            fillCodeLengths(node.left, depth + 1, codeLen);
            fillCodeLengths(node.right, depth + 1, codeLen);
        }
    }

    // Build canonical codes from code lengths
    public static void buildCanonicalCodes(int[] codeLen, int[] outCodeBits) {
        int K = codeLen.length;

        // Collect (symbol, length) for nonzero lengths
        List<int[]> syms = new ArrayList<>();
        for (int s = 0; s < K; s++) {
            int len = codeLen[s];
            if (len > 0) {
                syms.add(new int[]{s, len});
            }
        }

        // Sort by length, then by symbol
        syms.sort((a, b) -> {
            int la = a[1], lb = b[1];
            if (la != lb) return Integer.compare(la, lb);
            return Integer.compare(a[0], b[0]);
        });

        int code = 0;
        int prevLen = 0;

        for (int[] pair : syms) {
            int symbol = pair[0];
            int len = pair[1];

            if (len > prevLen) {
                code <<= (len - prevLen);
                prevLen = len;
            }
            outCodeBits[symbol] = code;
            code++;
        }
    }

    // Build a tree used only for decoding, from canonical codes
    public static HuffmanNode buildDecodingTree(int[] codeLen, int[] codeBits) {
        HuffmanNode root = new HuffmanNode(-1, 0, null, null);

        int K = codeLen.length;
        for (int s = 0; s < K; s++) {
            int len = codeLen[s];
            if (len <= 0) continue;
            int code = codeBits[s];

            HuffmanNode node = root;
            for (int i = len - 1; i >= 0; i--) {
                int bit = (code >>> i) & 1;
                if (bit == 0) {
                    if (node.left == null) {
                        node.left = new HuffmanNode(-1, 0, null, null);
                    }
                    node = node.left;
                } else {
                    if (node.right == null) {
                        node.right = new HuffmanNode(-1, 0, null, null);
                    }
                    node = node.right;
                }
            }
            // Leaf
            node.symbol = s;
        }
        return root;
    }

    // Decode one symbol from bits using the tree
    public static int decodeSymbol(BitInputStream bin, HuffmanNode root) throws java.io.IOException {
        HuffmanNode node = root;
        while (!node.isLeaf()) {
            int bit = bin.readBit();
            if (bit == -1) {
                throw new java.io.EOFException("Unexpected EOF in Huffman bitstream");
            }
            node = (bit == 0) ? node.left : node.right;
        }
        return node.symbol;
    }
}
