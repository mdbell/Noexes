package me.mdbell.noexs.misc;

import java.util.AbstractList;

public class OffsetList extends AbstractList<Long> {

    private long base;
    private int size;
    private int dataSize;

    public OffsetList(long base, int size, int dataSize) {
        this.base = base;
        this.size = size;
        this.dataSize = dataSize;
    }

    @Override
    public Long get(int index) {
        if(index >= size) {
            throw new IndexOutOfBoundsException();
        }
        return base + index * dataSize;
    }

    @Override
    public int size() {
        return size;
    }
}
