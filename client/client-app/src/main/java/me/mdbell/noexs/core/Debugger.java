package me.mdbell.noexs.core;

import me.mdbell.noexs.misc.BreakpointFlagBuilder;
import me.mdbell.noexs.misc.BreakpointType;
import me.mdbell.noexs.misc.WatchpointFlagBuilder;
import me.mdbell.noexs.ui.NoexsApplication;
import me.mdbell.noexs.ui.models.DataType;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class Debugger implements Commands, Closeable {

    private IConnection conn;
    private MemoryInfo prev;
    private Semaphore semaphore = new Semaphore(1);
    private int protocolVersion;

    public static final int CURRENT_PROTOCOL_VERSION = (NoexsApplication.VERSION_MAJOR << 16) | (NoexsApplication.VERSION_MINOR) << 8;

    public Debugger(IConnection conn) {
        this.conn = conn;
    }

    public IConnection raw() {
        return conn;
    }

    private void acquire() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new ConnectionException(e);
        }
    }

    private void release() {
        semaphore.release();
    }

    public DebuggerStatus getStatus() {
        acquire();
        try {
            conn.writeCommand(COMMAND_STATUS);
            conn.flush();
            int status = conn.readByte();
            int major = conn.readByte();
            int minor = conn.readByte();
            int patch = conn.readByte();
            this.protocolVersion = (major << 16) | (minor << 8);
            Result rc = conn.readResult();
            if (rc.failed()) {
                throw new ConnectionException("This is impossible, so you've done something terribly wrong", rc);
            }
            if(protocolVersion > CURRENT_PROTOCOL_VERSION) {
                throw new ConnectionException(String.format("Unsupported protocol version:%08X", protocolVersion));
            }
            protocolVersion |= patch; // we don't need to check the patch value, as it should always be backwards compatible.
            return DebuggerStatus.forId(status);
        } finally {
            release();
        }
    }

    public void poke(DataType type, long addr, long value) {
        switch (type) {
            case BYTE:
                poke8(addr, (int) value);
                break;
            case SHORT:
                poke16(addr, (int) value);
                break;
            case INT:
                poke32(addr, (int) value);
                break;
            case LONG:
                poke64(addr, value);
                break;
        }
    }

    public void poke8(long addr, int value) {
        acquire();
        try {
            conn.writeCommand(COMMAND_POKE8);
            conn.writeLong(addr);
            conn.writeByte(value);
            conn.flush();
            Result rc = conn.readResult();
            if (rc.failed()) {
                throw new ConnectionException(rc);
            }
        } finally {
            release();
        }
    }

    private byte[] peekBuffer = new byte[8];

    public int peek8(long addr) {
        ByteBuffer b = readmem(addr, 1, peekBuffer).order(ByteOrder.LITTLE_ENDIAN);
        return b.get() & 0xFF;
    }

    public void poke16(long addr, int value) {
        acquire();
        try {
            conn.writeCommand(COMMAND_POKE16);
            conn.writeLong(addr);
            conn.writeShort(value);
            conn.flush();
            Result rc = conn.readResult();
            if (rc.failed()) {
                throw new ConnectionException(rc);
            }
        } finally {
            release();
        }
    }

    public int peek16(long addr) {
        ByteBuffer b = readmem(addr, 2, peekBuffer).order(ByteOrder.LITTLE_ENDIAN);
        return b.getShort() & 0xFFFF;

    }

    public void poke32(long addr, int value) {
        acquire();
        try {
            conn.writeCommand(COMMAND_POKE32);
            conn.writeLong(addr);
            conn.writeInt(value);
            conn.flush();
            Result rc = conn.readResult();
            if (rc.failed()) {
                throw new ConnectionException(rc);
            }
        } finally {
            release();
        }
    }

    public int peek32(long addr) {
        ByteBuffer b = readmem(addr, 4, peekBuffer).order(ByteOrder.LITTLE_ENDIAN);
        return b.getInt();
    }

    public void poke64(long addr, long value) {
        acquire();
        try {
            conn.writeCommand(COMMAND_POKE32);
            conn.writeLong(addr);
            conn.writeLong(value);
            conn.flush();
            Result rc = conn.readResult();
            if (rc.failed()) {
                throw new ConnectionException(rc);
            }
        } finally {
            release();
        }
    }

    public long peek64(long addr) {
        ByteBuffer b = readmem(addr, 8, peekBuffer).order(ByteOrder.LITTLE_ENDIAN);
        return b.getLong();
    }

    public Result setWatchpoint(boolean read, boolean write, long addr) {
        acquire();
        try {
            Result rc;

            WatchpointFlagBuilder.MatchType t;
            if (read & write) {
                t = WatchpointFlagBuilder.MatchType.ALL;
            } else if (read) {
                t = WatchpointFlagBuilder.MatchType.LOAD;
            } else if (write) {
                t = WatchpointFlagBuilder.MatchType.STORE;
            } else {
                throw new IllegalArgumentException("No flags set for watchpoint");
            }
            System.out.println(t);
            int size = 4;
            int id = 0;
            int bpId = id + 4;
            int offset = (int) (addr - (addr & ~3));
            int mask = ((1 << size) - 1) << offset;

            BreakpointFlagBuilder bp = new BreakpointFlagBuilder().setEnabled(true)
                    .setAddressSelect(0xF).setBreakpointType(BreakpointType.LINKED_CONTEXT_IDR_MATCH);
            WatchpointFlagBuilder wp = new WatchpointFlagBuilder().setEnabled(true)
                    .setAccessContol(t).setAddressSelect(mask).setLinkedBreakpointNumber(bpId);
            rc = setBreakpoint(bpId, bp.getFlag(), 0);
            System.out.println("bp:" + rc);
            if (rc.succeeded()) {
                rc = setBreakpoint(0x10 + id, wp.getFlag(), addr); //wp
                System.out.println("wp:" + rc);
            }
            return rc;
        } finally {
            release();
        }
    }

    public Result setBreakpoint(int id, long flags, long addr) {
        acquire();
        try {
            conn.writeCommand(COMMAND_SET_BREAKPOINT);
            conn.writeInt(id);
            conn.writeLong(addr);
            conn.writeLong(flags);
            conn.flush();
            return conn.readResult();
        } finally {
            release();
        }
    }

    public Result writemem(byte[] data, long addr) {
        return writemem(data, 0, data.length, addr);
    }

    public Result writemem(byte[] data, int off, int len, long addr) {
        acquire();
        try {
            conn.writeCommand(COMMAND_WRITE);
            conn.writeLong(addr);
            conn.writeInt(len);
            conn.flush();
            Result r = conn.readResult();
            if (r.succeeded()) {
                conn.write(data, off, len);
                conn.flush();
            } else {
                conn.readResult();
                return r;
            }
            return conn.readResult();
        } finally {
            release();
        }
    }

    public void readmem(MemoryInfo info, OutputStream to) throws IOException {
        if (!info.isReadable()) {
            return;
        }
        readmem(info.getAddress(), (int) info.getSize(), to);
    }

    public void readmem(long start, int size, OutputStream to) throws IOException {
        acquire();
        try {
            conn.writeCommand(COMMAND_READ);
            conn.writeLong(start);
            conn.writeInt(size);
            conn.flush();

            Result rc = conn.readResult();
            if (rc.succeeded()) {
                byte[] buffer = new byte[2048 * 4];
                while (size > 0) {
                    rc = conn.readResult();
                    if (rc.failed()) {
                        conn.readResult();
                        throw new ConnectionException(rc);
                    }
                    int len = readCompressed(buffer);
                    to.write(buffer, 0, len);
                    size -= len;
                }
            }
            conn.readResult();
        } finally {
            release();
        }
    }

    public ByteBuffer readmem(long addr, int size, byte[] bytes) {
        acquire();
        try {
            conn.writeCommand(COMMAND_READ);
            conn.writeLong(addr);
            conn.writeInt(size);
            conn.flush();
            Result rc = conn.readResult();

            if (rc.failed()) {
                conn.readResult(); // ignored
                throw new ConnectionException(rc);
            }

            if (bytes == null) {
                bytes = new byte[size];
            }

            int pos = 0;
            byte[] buffer = new byte[2048 * 4];
            while (pos < size) {
                rc = conn.readResult();
                if (rc.failed()) {
                    conn.readResult();
                    throw new ConnectionException(rc);
                }
                int len = readCompressed(buffer);
                System.arraycopy(buffer, 0, bytes, pos, len);
                pos += len;
            }
            conn.readResult(); // ignored
            return ByteBuffer.wrap(bytes);
        } finally {
            release();
        }
    }

    public Result resume() {
        return getResult(COMMAND_CONTINUE);
    }

    public Result pause() {
        return getResult(COMMAND_PAUSE);
    }

    public Result attach(long pid) {
        acquire();
        try {
            conn.writeCommand(COMMAND_ATTACH);
            conn.writeLong(pid);
            conn.flush();
            return conn.readResult();
        } finally {
            release();
        }
    }

    public Result detach() {
        return getResult(COMMAND_DETATCH);
    }

    public MemoryInfo query(long address) {
        acquire();
        try {
            if (prev != null && prev.getAddress() != 0 && address >= prev.getAddress() && address < prev.getNextAddress()) {
                return prev;
            }

            conn.writeCommand(COMMAND_QUERY_MEMORY);
            conn.writeLong(address);
            conn.flush();
            return prev = readInfo();
        } finally {
            release();
        }
    }

    public MemoryInfo[] query(long start, int max) {
        acquire();
        try {
            conn.writeCommand(COMMAND_QUERY_MEMORY_MULTI);
            conn.writeLong(start);
            conn.writeInt(max);
            conn.flush();

            MemoryInfo[] res = new MemoryInfo[max];
            int count;
            for (count = 0; count < max; count++) {
                MemoryInfo info = readInfo();
                res[count] = info;
                if (info.getType() == MemoryType.RESERVED) {
                    break;
                }
            }
            conn.readResult(); // ignored here, it gets checked in readInfo()
            return Arrays.copyOf(res, count);
        } finally {
            release();
        }
    }

    public long getCurrentPid() {
        acquire();
        try {
            conn.writeCommand(COMMAND_CURRENT_PID);
            conn.flush();
            long pid = conn.readLong();
            Result rc = conn.readResult();
            if (rc.failed()) {
                pid = 0;
            }
            return pid;
        } finally {
            release();
        }
    }

    public long getAttachedPid() {
        acquire();
        try {
            conn.writeCommand(COMMAND_GET_ATTACHED_PID);
            conn.flush();
            long pid = conn.readLong();
            Result rc = conn.readResult();
            if (rc.failed()) {
                throw new ConnectionException("This is impossible, so you've done something terribly wrong", rc);
            }
            return pid;
        } finally {
            release();
        }
    }

    public long[] getPids() {
        acquire();
        try {
            conn.writeCommand(COMMAND_GET_PIDS);
            conn.flush();
            int count = conn.readInt();
            long[] pids = new long[count];
            for (int i = 0; i < count; i++) {
                pids[i] = conn.readLong();
            }
            Result rc = conn.readResult();
            if (rc.failed()) {
                throw new ConnectionException(rc);
            }
            return pids;
        } finally {
            release();
        }
    }

    public long getTitleId(long pid) {
        acquire();
        try {
            conn.writeCommand(COMMAND_GET_TITLEID);
            conn.writeLong(pid);
            conn.flush();
            long tid = conn.readLong();
            Result rc = conn.readResult();
            if (rc.failed()) {
                //TODO throw? idk
            }
            return tid;
        } finally {
            release();
        }
    }

    private void disconnect() {
        acquire();
        try {
            conn.writeCommand(COMMAND_DISCONNECT);
            conn.flush();
            Result rc = conn.readResult();
            if (rc.failed()) {
                throw new ConnectionException("This is impossible, so you've done something terribly wrong", rc);
            }
        } finally {
            release();
        }
    }

    public long getCurrentTitleId() {
        long pid = getCurrentPid();
        if (pid == 0) {
            return 0;
        }
        return getTitleId(pid);
    }

    public boolean attached() {
        return getAttachedPid() != 0;
    }

    public boolean connected() {
        return conn.connected();
    }

    @Override
    public void close() throws IOException {
        if (connected()) {
            detach();
            disconnect();
        }
        conn.close();
    }

    private byte[] compressedBuffer = new byte[2048 * 4 * 6];

    private int readCompressed(byte[] buffer) {
        int compressedFlag = conn.readByte();
        int decompressedLen = conn.readInt();

        if (compressedFlag == 0) {
            conn.readFully(buffer, 0, decompressedLen);
        } else {
            int compressedLen = conn.readInt();
            conn.readFully(compressedBuffer, 0, compressedLen);
            int pos = 0;
            for (int i = 0; i < compressedLen; i += 2) {
                byte value = compressedBuffer[i];
                int count = compressedBuffer[i + 1] & 0xFF;
                Arrays.fill(buffer, pos, pos + count, value);
                pos += count;
            }
        }
        return decompressedLen;
    }

    private MemoryInfo readInfo() {
        long addr = conn.readLong();
        long size = conn.readLong();
        int type = conn.readInt();
        int perm = conn.readInt();
        Result rc = conn.readResult();
        if (rc.failed()) {
            throw new ConnectionException(rc);
        }
        return new MemoryInfo(addr, size, type, perm);
    }

    private Result getResult(int cmd) {
        acquire();
        try {
            conn.writeCommand(cmd);
            conn.flush();
            return conn.readResult();
        } finally {
            release();
        }
    }

    public long peek(DataType type, long addr) {
        switch (type) {
            case BYTE:
                return peek8(addr);
            case SHORT:
                return peek16(addr);
            case INT:
                return peek32(addr);
            case LONG:
                return peek64(addr);
        }
        throw new IllegalArgumentException("Illegal data type:" + type);
    }
}