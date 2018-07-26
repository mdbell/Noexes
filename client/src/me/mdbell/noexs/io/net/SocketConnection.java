package me.mdbell.noexs.io.net;

import me.mdbell.noexs.core.ConnectionException;
import me.mdbell.noexs.core.IConnection;

import java.io.*;
import java.net.Socket;
//TODO handle exceptions in this class properly
public class SocketConnection implements IConnection {

    private Socket socket;
    private InputStream in;
    private OutputStream out;

    public SocketConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedInputStream(socket.getInputStream());
        this.out = new BufferedOutputStream(socket.getOutputStream());
    }

    @Override
    public boolean connected() {
        return socket.isConnected() && !socket.isClosed();
    }

    @Override
    public void writeByte(int i) {
        try {
            out.write(i);
        } catch (IOException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public void write(byte[] data, int off, int len) {
        try {
            out.write(data, off, len);
        } catch (IOException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public int readByte() {
        try {
            return in.read();
        } catch (IOException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) {
        try {
            return in.read(b, off, len);
        } catch (IOException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public void close() throws IOException {
        flush();
        socket.close();
    }

    public void flush() {
        try {
            out.flush();
        } catch (IOException e) {
            throw new ConnectionException(e);
        }
    }
}