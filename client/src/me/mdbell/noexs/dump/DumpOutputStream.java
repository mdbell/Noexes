package me.mdbell.noexs.dump;

import java.io.*;
import java.util.List;

public class DumpOutputStream extends OutputStream {

    private DumpIndex curr;
    private List<DumpIndex> indices;
    private RandomAccessFile dataFile;
    private MemoryDump from;

    DumpOutputStream(MemoryDump from, List<DumpIndex> indices, RandomAccessFile data) {
        this.from = from;
        this.indices = indices;
        this.dataFile = data;
    }

    public void setCurrentAddress(long addr) throws IOException {
        if (curr != null) {
            curr.size = dataFile.getFilePointer() - curr.filePos;
            indices.add(curr);
        }
        curr = new DumpIndex();
        curr.addr = addr;
        curr.filePos = dataFile.getFilePointer();
    }

    @Override
    public void write(int b) throws IOException {
        dataFile.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        dataFile.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        dataFile.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        if (curr != null) {
            curr.size = dataFile.getFilePointer() - curr.filePos;
            indices.add(curr);
        }
        from.writeHeader();
        dataFile = null;
    }
}
