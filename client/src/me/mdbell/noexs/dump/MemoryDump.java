package me.mdbell.noexs.dump;

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
    private final File location;

    public MemoryDump(File where) throws IOException {
        location = where;
        File data = new File(where,"dump.dat");
        dump = new RandomAccessFile(data, "rw");
    }

    public MemoryDump(File from, List<DumpIndex> indices) throws FileNotFoundException {
        this.location = from.getParentFile();
        this.dump = new RandomAccessFile(from, "rw");
        this.indices.addAll(indices);
    }

    public DumpOutputStream openStream() {
        return new DumpOutputStream(this, indices, dump);
    }

    protected void saveIndices() throws IOException {
        IndexSerializer.write(indices, new File(location, "indices.xml").toPath());
    }

    @Override
    public void close() throws IOException {
        cache.clear();
        indices.clear();
        dump.close();
        System.gc();
    }

    public List<DumpIndex> getIndices(){
        return Collections.unmodifiableList(indices);
    }

    public List<DumpRegion> getIndicesAsRegions(){
        List<DumpRegion> res = new ArrayList<>();
        for(DumpIndex idx : indices) {
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

    public long getEnd(){
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

    private ThreadLocal<DumpIndex> prev = ThreadLocal.withInitial(() -> null);

    public DumpIndex getIndex(long addr){
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
