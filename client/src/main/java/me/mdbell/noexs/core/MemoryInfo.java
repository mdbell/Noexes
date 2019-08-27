package me.mdbell.noexs.core;

public class MemoryInfo {
    private long addr; //64
    private long size; //64
    private MemoryType type; //32
    private int perm; //32

    public MemoryInfo(long addr, long size, int type, int perm) {
        this.addr = addr;
        this.size = size;
        this.type = MemoryType.valueof(type);
        this.perm = perm;
    }

    public long getAddress() {
        return addr;
    }

    public long getSize() {
        return size;
    }

    public long getNextAddress() {
        return addr + size;
    }

    public MemoryType getType() {
        return type;
    }

    public int getPerm() {
        return perm;
    }

    public boolean isReadable() {
        return (perm & 1) != 0;
    }

    public boolean isWriteable() {
        return (perm & 2) != 0;
    }

    public boolean isExecutable() {
        return (perm & 4) != 0;
    }

    public boolean contains(long addr) {
        return getAddress() <= addr && getNextAddress() > addr;
    }


    @Override
    public String toString() {
        return "MemoryInfo{" +
                "addr=0x" + Long.toUnsignedString(addr, 16) +
                ", size=0x" + Long.toUnsignedString(size, 16) +
                ", type=" + type +
                ", perm=" + perm +
                '}';
    }
}
