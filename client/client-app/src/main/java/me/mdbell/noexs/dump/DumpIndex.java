package me.mdbell.noexs.dump;

import me.mdbell.util.HexUtils;

import java.util.Objects;

public final class DumpIndex {

    protected long addr;
    protected long filePos;
    protected long size;

    protected DumpIndex(){

    }

    public DumpIndex(long addr, long pos, long size) {
        this.addr = addr;
        this.filePos = pos;
        this.size = size;
    }

    @Override
    public String toString() {
        return "DumpIndex{" +
                "addr=" + HexUtils.formatAddress(addr) +
                ", filePos=" + filePos +
                ", size=" + size +
                '}';
    }

    public long getAddress() {
        return addr;
    }

    public long getEndAddress(){
        return getAddress() + getSize();
    }

    public long getSize(){
        return size;
    }

    public long getFilePos() {
        return filePos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DumpIndex dumpIndex = (DumpIndex) o;
        return addr == dumpIndex.addr &&
                filePos == dumpIndex.filePos &&
                size == dumpIndex.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(addr, filePos, size);
    }
}
