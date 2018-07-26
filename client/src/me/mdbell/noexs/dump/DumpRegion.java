package me.mdbell.noexs.dump;

import me.mdbell.util.HexUtils;

public class DumpRegion {

    private long start;
    private long end;

    public DumpRegion(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public long getSize() {
        return end - start;
    }

    @Override
    public String toString() {
        return "DumpRegion{" +
                "start=" + HexUtils.formatAddress(start) +
                ", end=" + HexUtils.formatAddress(end) +
                '}';
    }
}
