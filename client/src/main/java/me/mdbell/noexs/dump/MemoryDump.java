package me.mdbell.noexs.dump;

import me.mdbell.noexs.core.MemoryInfo;
import me.mdbell.noexs.misc.IndexSerializer;
import me.mdbell.util.HexUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryDump implements Closeable {

    private final RandomAccessFile dump;
    private final Map<DumpIndex, MappedByteBuffer> cache = new ConcurrentHashMap<>();
    private final List<DumpIndex> indices = new ArrayList<>();
    private final List<MemoryInfo> infos = new ArrayList<>();
    private long tid;

    private ThreadLocal<DumpIndex> prev = ThreadLocal.withInitial(() -> null);

    public MemoryDump(File from) throws IOException {
        boolean b = from.exists() && from.length() > 0;
        this.dump = new RandomAccessFile(from, "rw");
        if (b) {
            readHeader();
        } else {
            writeHeader();
        }
    }

    void writeHeader() throws IOException {
        long pos = dump.length();
        dump.seek(0);
        dump.writeInt(0x4E444D50); // "NDMP"
        dump.writeLong(tid); // TID
        dump.writeInt(infos.size()); // info count
        dump.writeLong(0); // mem-info pointer
        dump.writeInt(indices.size()); // index count
        dump.writeLong(pos); // pointer to index data
        dump.seek(pos);
        for (int i = 0; i < indices.size(); i++) {
            DumpIndex idx = indices.get(i);
            dump.writeLong(idx.addr);
            dump.writeLong(idx.filePos);
            dump.writeLong(idx.size);
        }
        pos = dump.getFilePointer();
        for(int i = 0; i < infos.size(); i++) {
            MemoryInfo info = infos.get(i);
            dump.writeLong(info.getAddress());
            dump.writeLong(info.getSize());
            dump.writeInt(info.getType().getType());
            dump.writeInt(info.getPerm());
        }
        dump.seek(0x10); // mem-info pointer (again)
        dump.writeLong(pos);

        dump.seek(0x32); // seek back to data
    }

    private void readHeader() throws IOException {
        dump.seek(0);
        if (dump.readInt() != 0x4E444D50) {
            throw new IOException("File is not a dump! (Invalid magic)");
        }
        tid = dump.readLong(); // TID

        int infoCount = dump.readInt();
        long infoPtr = dump.readLong();

        int idxCount = dump.readInt();
        long idxPtr = dump.readLong();
        long dataPtr = dump.getFilePointer();
        dump.seek(idxPtr);
        for (int i = 0; i < idxCount; i++) {
            long addr = dump.readLong();
            long pos = dump.readLong();
            long size = dump.readLong();
            indices.add(new DumpIndex(addr, pos, size));
        }

        dump.seek(infoPtr);
        for(int i = 0; i < infoCount; i++) {
            long addr = dump.readLong();
            long size = dump.readLong();
            int type = dump.readInt();
            int perm = dump.readInt();
            infos.add(new MemoryInfo(addr, size, type, perm));
        }
        dump.seek(dataPtr);
    }

    public DumpOutputStream openStream() {
        return new DumpOutputStream(this, indices, dump);
    }

    @Override
    public void close() throws IOException {
        writeHeader();
        cache.clear();
        indices.clear();
        dump.close();
        System.gc();
    }

    public List<MemoryInfo> getInfos(){
        return infos;
    }

    public List<DumpIndex> getIndices() {
        return Collections.unmodifiableList(indices);
    }

    public List<DumpRegion> getIndicesAsRegions() {
        List<DumpRegion> res = new ArrayList<>();
        for (DumpIndex idx : indices) {
            res.add(new DumpRegion(idx.getAddress(), idx.getEndAddress()));
        }
        return res;
    }

    public long getSize() throws IOException {
        return dump.length();
    }

    public long getStart() {
        long min = Long.MAX_VALUE;
        for (DumpIndex idx : getIndices()) {
            long addr = idx.addr;
            if (addr < min) {
                min = addr;
            }
        }
        return min;
    }

    public long getEnd() {
        long max = 0;
        for (DumpIndex idx : getIndices()) {
            long addr = idx.addr + idx.size;
            if (addr >= max) {
                max = addr;
            }
        }
        return max;
    }

    public ByteBuffer getBuffer(DumpIndex idx) throws IOException {
        if (cache.containsKey(idx)) {
            MappedByteBuffer b = cache.get(idx);
            b.position(0);
            return b.duplicate().order(b.order());
        }
        FileChannel channel = dump.getChannel();
        MappedByteBuffer res = channel.map(FileChannel.MapMode.READ_ONLY, idx.filePos, idx.size);
        res.order(ByteOrder.LITTLE_ENDIAN);
        cache.put(idx, res);
        return res.duplicate().order(res.order());
    }

    public ByteBuffer getBuffer(long addr) throws IOException {
        DumpIndex idx = getIndex(addr);
        ByteBuffer buffer = getBuffer(idx);
        buffer.position((int) (addr - idx.addr));
        return buffer;
    }

    public long getValue(long addr, int size) throws IOException {
        ByteBuffer buffer = getBuffer(addr);
        switch (size) {
            case 1:
                return buffer.get() & 0xFF;
            case 2:
                return buffer.getShort() & 0xFFFF;
            case 4:
                return buffer.getInt() & 0xFFFFFFFFL;
            case 8:
                return buffer.getLong();
            default:
                throw new UnsupportedOperationException("invalid size:" + size);
        }
    }


    public long getTid(){
        return tid;
    }

    public void setTid(long tid){
        this.tid = tid;
    }

    public DumpIndex getIndex(long addr) {
        DumpIndex prev = this.prev.get();
        if (prev != null && addr >= prev.addr && addr < prev.addr + prev.size) {
            return prev;
        }
        List<DumpIndex> indices = getIndices();
        for (DumpIndex idx : indices) {
            if (addr >= idx.addr && addr < idx.addr + idx.size) {
                this.prev.set(idx);
                return idx;
            }
        }
        return null;
    }
}
