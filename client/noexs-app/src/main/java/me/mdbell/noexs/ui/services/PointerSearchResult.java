package me.mdbell.noexs.ui.services;

import me.mdbell.util.HexUtils;

import java.io.Closeable;
import java.util.Objects;

public class PointerSearchResult implements Comparable<PointerSearchResult>, Cloneable {

    PointerSearchResult prev;
    int depth;
    long address;
    long offset;

    public PointerSearchResult(long address, long offset) {
        this.address = address;
        this.offset = offset;
        this.depth = 0;
    }

    public String formatted(long main) {
        StringBuilder prefix = new StringBuilder();
        StringBuilder suffix = new StringBuilder();

        PointerSearchResult psr = prev;
        while (psr != null) {
            prefix.append('[');
            suffix.append(']');
            if (psr.offset != 0) {
                suffix.append(' ').append(prev.offset < 0 ? '-' : '+').append(" ").append(Long.toUnsignedString(Math.abs(prev.offset), 16));
            }

            psr = prev.prev;
        }

        String str = "[";

        if (main != 0) {
            long rel = address - main;
            str = str + "main" + (rel >= 0 ? "+" : "-") + Long.toUnsignedString(Math.abs(rel), 16);

        } else {
            str = str + HexUtils.formatAddress(address);
        }

        str = str + "]";

        if (offset != 0) {
            str = str + " " + (offset < 0 ? " - " : "+ ") + Long.toUnsignedString(Math.abs(offset), 16);
        }

        return prefix.toString() + str + suffix.toString();
    }

    @Override
    public int compareTo(PointerSearchResult o) {
        int depthDiff = depth - o.depth;
        if (depthDiff != 0) {
            return depthDiff;
        }
        long addrDiff = address = o.address;
        if (addrDiff != 0) {
            return (int) addrDiff;
        }

        long offsetDiff = offset - o.offset;
        if (offsetDiff != 0) {
            return (int) offsetDiff;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointerSearchResult that = (PointerSearchResult) o;
        return that.formatted(0).equals(formatted(0)); //TODO not this.
    }

    @Override
    public int hashCode() {
        return formatted(0).hashCode();
    }

    @Override
    protected Object clone() {
        PointerSearchResult psr = new PointerSearchResult(address, offset);
        psr.depth = depth;
        psr.prev = prev == null ? null : (PointerSearchResult) prev.clone();
        return psr;
    }

    public long getAddress() {
        return address;
    }
}
