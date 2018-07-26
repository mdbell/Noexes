package me.mdbell.noexs.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public abstract class MappedList<T> extends AbstractList<T> implements Closeable {

    private static final long BUFFER_SIZE = 1024 * 1024 * 50; //50MB

    private RandomAccessFile raf;
    private List<MappedByteBuffer> buffers = new ArrayList<>();
    private int size;
    private final long dataSize;

    private ThreadLocal<Integer> cachedIdx = ThreadLocal.withInitial(() -> -1);
    private ThreadLocal<MappedByteBuffer> cachedBuffer = ThreadLocal.withInitial(() -> null);

    protected abstract long dataSize();

    protected abstract T read(ByteBuffer from, int pos);

    protected abstract boolean write(ByteBuffer to, int pos, T value);

    public MappedList(RandomAccessFile raf) {
        this(raf, 0);
    }

    public MappedList(RandomAccessFile raf, int size) {
        this.raf = raf;
        this.size = size;
        this.dataSize = dataSize();
    }

    private int wrapIndex(int index) {
        return (int) ((index * dataSize) % BUFFER_SIZE);
    }

    private synchronized void checkSize(int index) {
        try {
            while (index >= buffers.size()) {
                MappedByteBuffer buffer = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, index * BUFFER_SIZE, BUFFER_SIZE);
                buffers.add(buffer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private MappedByteBuffer getBuffer(int idx) {
        long pos = idx * dataSize;
        int bufferIndex = (int) (pos / BUFFER_SIZE);
        int cachedIdx = this.cachedIdx.get();
        if (bufferIndex == cachedIdx) {
            return cachedBuffer.get();
        }
        if (bufferIndex >= buffers.size()) {
            checkSize(bufferIndex);
        }
        this.cachedIdx.set(bufferIndex);
        MappedByteBuffer buffer = buffers.get(bufferIndex);
        this.cachedBuffer.set(buffer);
        return buffer;
    }

    @Override
    public T get(int index) {
        return read(getBuffer(index), wrapIndex(index));
    }

    @Override
    public T set(int index, T element) {
        write(getBuffer(index), wrapIndex(index), element);
        return element;
    }

    @Override
    public boolean add(T value) {
        int index = size++;
        set(index, value);
        return true;
    }

    @Override
    public void close() throws IOException {
        cachedBuffer = null;
        buffers.clear();
        System.gc();
        raf.close();
    }

    @Override
    public int size() {
        return size;
    }

    public static MappedList<Long> createLongList(RandomAccessFile raf) {
        return new MappedList<>(raf) {
            @Override
            protected long dataSize() {
                return Long.BYTES;
            }

            @Override
            protected Long read(ByteBuffer from, int pos) {
                return from.getLong(pos);
            }

            @Override
            protected boolean write(ByteBuffer to, int pos, Long value) {
                to.putLong(value);
                return true;
            }
        };
    }
}
