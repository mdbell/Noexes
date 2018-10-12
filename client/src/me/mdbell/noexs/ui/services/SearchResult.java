package me.mdbell.noexs.ui.services;

import me.mdbell.noexs.dump.DumpRegion;
import me.mdbell.noexs.ui.NoexesFiles;
import me.mdbell.noexs.dump.MemoryDump;
import me.mdbell.noexs.ui.models.DataType;
import me.mdbell.noexs.ui.models.SearchType;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

public final class SearchResult implements Closeable {

    private static final int PAGE_SIZE = 1024;

    private File location;
    List<Long> addresses;
    List<DumpRegion> regions;
    DataType dataType;

    SearchType type;

    long start;
    long end;
    MemoryDump curr;

    private SearchResult prev;

    public SearchResult() throws IOException {
        this.location = NoexesFiles.createTempFile("dmp");
    }

    public File getLocation() {
        return location;
    }

    public List<Long> getAddresses() {
        return addresses;
    }

    public long getStart() {
        return start;
    }

    public DataType getDataType(){
        return dataType;
    }

    public SearchType getType() {
        return type;
    }

    public long getEnd() {
        return end;
    }

    public long getCurr(long addr) throws IOException {
        return curr.getValue(addr, dataType.getSize());
    }

    public long getPrev(long addr) throws IOException {
        if (prev == null) {
            return getCurr(addr);
        }
        return prev.getCurr(addr);
    }

    public int size() {
        return addresses.size();
    }

    public List<Long> getPage(int idx) {
        System.out.println(PAGE_SIZE * idx + " - " + Math.min(size(), PAGE_SIZE * (idx + 1)));
        return addresses.subList(PAGE_SIZE * idx, Math.min(size(), PAGE_SIZE * (idx + 1)));
    }

    public int getPageCount() {
        int size = size();
        if (size == 0) {
            return 0;
        }
        if(size % PAGE_SIZE != 0) {
            size += PAGE_SIZE;
        }
        return size / PAGE_SIZE;
    }

    @Override
    public void close() throws IOException {
        if(curr != null) {
            curr.close();
        }
        if(prev != null) {
            prev.close();
        }
        if(addresses instanceof Closeable) {
            ((Closeable) addresses).close();
        }
    }

    public void setPrev(SearchResult prev) {
        this.prev = prev;
    }

    public SearchResult getPrev(){
        return prev;
    }
}
