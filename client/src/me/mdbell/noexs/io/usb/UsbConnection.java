package me.mdbell.noexs.io.usb;

import me.mdbell.noexs.core.ConnectionException;
import me.mdbell.noexs.core.IConnection;

import javax.usb.*;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class UsbConnection implements IConnection {

    private static final byte USB_INTERFACE = 1;

    private static final byte READ_ENDPOINT = (byte) 0x83;
    private static final byte WRITE_ENDPOINT = (byte) 0x03;

    private UsbDevice device;
    private UsbConfiguration cfg;
    private UsbInterface iface;
    private UsbEndpoint read, write;
    private UsbPipe readPipe, writePipe;

    private List<UsbIrp> outputIrps = new LinkedList<>();

    public UsbConnection(UsbDevice device) throws UsbException {
        this.device = device;
        init();
    }

    private void init() throws UsbException {
        cfg = device.getActiveUsbConfiguration();
        iface = cfg.getUsbInterface(USB_INTERFACE);
        iface.claim();
        read = iface.getUsbEndpoint(READ_ENDPOINT);
        write = iface.getUsbEndpoint(WRITE_ENDPOINT);
        readPipe = read.getUsbPipe();
        writePipe = write.getUsbPipe();

        readPipe.open();
        writePipe.open();
    }

    @Override
    public boolean connected() {
        return readPipe.isOpen() && writePipe.isOpen();
    }

    @Override
    public void writeByte(int i) {
        UsbIrp irp = writePipe.createUsbIrp();
        irp.setData(new byte[]{(byte) i});
        outputIrps.add(irp);
    }

    @Override
    public void write(byte[] data, int off, int len) {
        UsbIrp irp = writePipe.createUsbIrp();
        irp.setData(data, off, len);
        outputIrps.add(irp);
    }

    @Override
    public int readByte() {
        byte[] b = {0};
        try {
            readPipe.syncSubmit(b);
        } catch (UsbException e) {
            throw new ConnectionException(e);
        }
        return b[0] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        UsbIrp irp = readPipe.createUsbIrp();
        irp.setData(b, off, len);
        try {
            readPipe.syncSubmit(irp);
        } catch (UsbException e) {
            throw new ConnectionException(e);
        }
        return irp.getActualLength();
    }

    @Override
    public void flush() {
        try {
            writePipe.syncSubmit(outputIrps);
            outputIrps.clear();
        } catch (UsbException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            readPipe.close();
            writePipe.close();
            iface.release();
        } catch (UsbException e) {
            throw new IOException(e);
        }
    }
}
