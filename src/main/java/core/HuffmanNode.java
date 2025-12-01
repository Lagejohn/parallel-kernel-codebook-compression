package main.java.core;

public final class HuffmanNode implements Comparable<HuffmanNode> {
    int symbol;   // -1 for internal nodes
    final int freq;
    HuffmanNode left;
    HuffmanNode right;

    HuffmanNode(int symbol, int freq, HuffmanNode left, HuffmanNode right) {
        this.symbol = symbol;
        this.freq = freq;
        this.left = left;
        this.right = right;
    }

    @Override
    public int compareTo(HuffmanNode other) {
        return Integer.compare(this.freq, other.freq);
    }

    boolean isLeaf() {
        return left == null && right == null;
    }
}
