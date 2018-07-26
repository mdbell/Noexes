package me.mdbell.noexs.misc;

import me.mdbell.noexs.core.IConnection;

import java.io.IOException;

public final class NopConnection implements IConnection {
    public static final NopConnection INSTANCE = new NopConnection();

    private NopConnection(){

    }

    @Override
    public boolean connected() {
        return false;
    }

    @Override
    public void writeByte(int i) {

    }

    @Override
    public void write(byte[] data, int off, int len) {

    }

    @Override
    public int readByte() {
        return -1;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        return -1;
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws IOException {

    }
}
