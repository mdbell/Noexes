package me.mdbell.noexs.core;

import java.io.Closeable;

public interface IConnection extends Closeable {

    boolean connected();

    default void writeCommand(int cmd) {
        writeByte(cmd);
    }

    default Result readResult() {
        return Result.valueOf(readInt());
    }

    default void write(byte[] b) {
        write(b, 0, b.length);
    }

    void writeByte(int i);

    void write(byte[] data, int off, int len);

    int readByte();

    default long readLong() {
        long l1 = readUInt();
        long l2 = readUInt();
        return l1 | l2 << 32L;
    }

    default long readUInt() {
        return Integer.toUnsignedLong(readInt());
    }

    default int readInt() {
        return readUShort() | readUShort() << 16;
    }

    default int readUShort() {
        return readShort() & 0xFFFF;
    }

    default short readShort() {
        byte[] data = new byte[2];
        int len = readFully(data);
        if (len != 2) {
            throw new ConnectionException("Unable to fully read data. Expected 2 bytes, but we only read:" + len);
        }
        return (short) ((data[0] & 0xFF) | (data[1] & 0xFF) << 8);
    }

    default void writeLong(long l) {
        writeInt((int) (l & 0xFFFFFFFFL));
        writeInt((int) (l >> 32 & 0xFFFFFFFFL));
    }

    default void writeInt(int i) {
        writeShort(i & 0xFFFF);
        writeShort((i >> 16) & 0xFFFF);
    }

    default void writeShort(int s) {
        writeByte(s & 0xFF);
        writeByte((s >> 8) & 0xFF);
    }

    default int read(byte[] b) {
        return read(b, 0, b.length);
    }

    int read(byte[] b, int off, int len);

    default int readFully(byte[] b) {
        return readFully(b, 0, b.length);
    }

    default int readFully(byte[] b, int off, int len) {
        int tmp = len;
        while (tmp > 0) {
            int i = read(b, off, tmp);
            if (i == -1) {
                break;
            }
            off += i;
            tmp -= i;
        }
        return len - tmp;
    }

    void flush();

}
