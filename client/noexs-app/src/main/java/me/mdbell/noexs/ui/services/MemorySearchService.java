package me.mdbell.noexs.ui.services;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import me.mdbell.noexs.core.Debugger;
import me.mdbell.noexs.core.DebuggerStatus;
import me.mdbell.noexs.core.MemoryInfo;
import me.mdbell.noexs.io.MappedList;
import me.mdbell.noexs.dump.*;
import me.mdbell.noexs.ui.NoexesFiles;
import me.mdbell.noexs.misc.DumpAddressList;
import me.mdbell.noexs.ui.models.ConditionType;
import me.mdbell.noexs.ui.models.DataType;
import me.mdbell.noexs.ui.models.SearchType;
import me.mdbell.util.NetUtils;
import me.mdbell.util.Rolling;
import me.mdbell.util.TimeUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MemorySearchService extends Service<SearchResult> {

    private DumpRegionSupplier supplier;
    private Debugger conn;

    private DataProvider provider;

    private DataType dataType;

    private SearchType type;
    private ConditionType compareType;

    private long knownValue;

    private SearchResult prevResult;

    public void clear() {
        supplier = null;
        conn = null;
        provider = null;
        type = null;
        compareType = null;
        knownValue = 0;
        if (prevResult != null) {
            try {
                prevResult.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        prevResult = null;
    }

    public void setDataType(DataType type) {
        switch (type) {
            case BYTE:
                provider = b -> b.get() & 0xFF;
                break;
            case SHORT:
                provider = b -> b.getShort() & 0xFFFF;
                break;
            case INT:
                provider = b -> b.getInt() & 0xFFFFFFFFL;
                break;
            case LONG:
                provider = ByteBuffer::getLong;
                break;
            default:
                throw new UnsupportedOperationException("Invalid/Unsupported dataType:" + type);
        }
        this.dataType = type;
    }

    public void setType(SearchType t) {
        this.type = t;
    }

    public void setCompareType(ConditionType type) {
        this.compareType = type;
    }

    public void setKnownValue(long value) {
        this.knownValue = value;
    }

    public void setPrevResult(SearchResult result) {
        this.prevResult = result;
    }

    @Override
    protected Task<SearchResult> createTask() {
        return new SearchTask();
    }

    private List<Long> createList(File where) {
        try {
            return MappedList.createLongList(new RandomAccessFile(NoexesFiles.createTempFile("addrs"), "rw"));
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void search(SearchResult result, ByteBuffer b, long baseAddress, DataProvider provider) throws IOException {
        while (b.hasRemaining()) {
            long addr = baseAddress + b.position();
            long current = provider.get(b);
            long prevValue = result.getPrev() == null ? -1 : result.getPrev(addr);
            if (condition(current, prevValue)) {
                result.addresses.add(addr);
                if (addr < result.start) {
                    result.start = addr;
                }
                if (addr > result.end) {
                    result.end = addr;
                }
            }
        }
    }

    private boolean condition(long value, long prev) {
        switch (type) {
            case PREVIOUS:
                return compare(value, prev);
            case KNOWN:
                return compare(value, knownValue);
            case DIFFERENT:
                return compare(Math.abs(value - prev), knownValue);
            default:
                throw new UnsupportedOperationException("Unsupported condition type:" + type);
        }
    }

    private boolean compare(long value, long other) {
        switch (compareType) {
            case EQUALS:
                return value == other;
            case NOT_EQUAL:
                return value != other;
            case LESS_THAN:
                return value < other;
            case LESS_THAN_OR_EQUAL:
                return value <= other;
            case GREATER_THAN:
                return value > other;
            case GREATER_OR_EQUAL:
                return value >= other;
            default:
                throw new UnsupportedOperationException("Unsupported comparison:" + compareType);
        }
    }

    public void setConnection(Debugger conn) {
        this.conn = conn;
    }

    public void setSupplier(DumpRegionSupplier supplier) {
        this.supplier = supplier;
    }

    interface DataProvider {
        long get(ByteBuffer from);
    }

    private class SearchTask extends Task<SearchResult> {

        SearchResult res;
        private long curr = 0, total, prevAmt;
        private long lastUpdate;
        Rolling avg = new Rolling(10);

        @Override
        protected SearchResult call() throws Exception {
            res = new SearchResult();
            res.type = type;
            res.dataType = dataType;
            res.setPrev(prevResult);
            res.addresses = createList(NoexesFiles.createTempFile("addrs"));
            if (prevResult != null) {
                refineSearch();
            } else {
                fullSearch();
            }
            return res;
        }

        private void update(long size) {
            curr += size;
            if (System.currentTimeMillis() - lastUpdate > 500) {
                lastUpdate = System.currentTimeMillis();
                avg.add(curr - prevAmt);
                prevAmt = curr;
            }
            double average = avg.getAverage() * 2;
            long remaining = total - curr;
            updateMessage(String.format("Refining... (%s/%s) ETA: %s", curr, total,
                    TimeUtils.formatTime((long) (remaining / average * 1000))));
            updateProgress(curr, total);
        }

        private void refineSearch() throws IOException {
            updateMessage("Computing regions...");
            DumpRegionSupplier supplier = computeRegions(prevResult);
            res.curr = createDump(res, supplier);
            total = prevResult.curr.getSize();

            if (prevResult.addresses.size() == 0) {
                return;
            }

            Iterator<Long> addrs = prevResult.addresses.iterator();
            Long addr = addrs.next();
            for (DumpIndex idx : res.curr.getIndices()) {
                if (!addrs.hasNext() || isCancelled()) {
                    break;
                }
                ByteBuffer buffer = res.curr.getBuffer(idx);
                while (buffer.hasRemaining() && !isCancelled()) {
                    long l = idx.getAddress() + buffer.position();
                    long value = provider.get(buffer);
                    if (l == addr) {
                        long prev = res.type.requiresPrevious() ? res.getPrev(l) : -1;
                        if (condition(value, prev)) {
                            res.addresses.add(l);
                            addAddress(addr);
                        }
                        addr = addrs.next();
                    } else while (l > addr && addrs.hasNext()) {
                        addr = addrs.next();
                    }
                }
                update(idx.getSize());
            }
        }

        private void fullSearch() throws Exception {
            res.curr = createDump(res, supplier);

            if (isCancelled()) {
                return;
            }

            if (type == SearchType.UNKNOWN) {
                res.addresses = new DumpAddressList(res.curr, res.dataType.getSize());
                res.regions = res.curr.getIndicesAsRegions();
                res.start = supplier.getStart();
                res.end = supplier.getEnd();
                return;
            }

            List<DumpIndex> indices = res.curr.getIndices();
            long read = 0;
            long size = res.curr.getSize();
            long start = supplier.getStart();
            for (DumpIndex idx : indices) {
                if (isCancelled() || idx.getAddress() >= supplier.getEnd()) {
                    break;
                }
                updateMessage("Searching...");
                ByteBuffer b = res.curr.getBuffer(idx);
                long addr = idx.getAddress();
                if (addr < start) {
                    b.position((int) (start - addr));
                }
                search(res, b, addr, provider);
                read += b.position();
                updateProgress(read, size);
            }
            return;
        }

        private DumpRegionSupplier computeRegions(SearchResult prev) {
            if (prev.regions != null) {
                return DumpRegionSupplier.createSupplier(prev.getStart(), prev.getEnd(), prev.regions, ((long) prev.size()) * prev.dataType.getSize());
            }
            MemoryInfo[] infos = conn.query(prev.getStart(), 10000);
            List<Long> addresses = prev.getAddresses();
            List<DumpRegion> regions = new ArrayList<>();
            DumpRegionFactory factory = null;
            long size = 0;
            long start = Long.MAX_VALUE;
            long end = 0;
            MemoryInfo info = null;
            for (int i = 0; i < addresses.size() && !isCancelled(); i++) {
                updateProgress(i, addresses.size());
                long addr = addresses.get(i);
                if (addr < start) {
                    start = addr;
                }
                if (addr > end) {
                    end = addr;
                }
                if (factory == null) {
                    factory = DumpRegionFactory.create().setStart(addr).setLength(dataType.getSize());
                    info = getInfo(infos, addr);
                } else {
                    long delta = addr - factory.getEnd();
                    if (delta <= 0x10000 && info.contains(addr)) {
                        factory.setEnd(addr + dataType.getSize());
                    } else {
                        size += factory.getLength();
                        regions.add(factory.build());
                        factory = DumpRegionFactory.create().setStart(addr).setLength(dataType.getSize());
                        info = getInfo(infos, addr);
                    }
                }
            }
            if (factory != null) {
                size += factory.getLength();
                regions.add(factory.build());
            }
            updateMessage(regions.size() + " regions computed.");
            return DumpRegionSupplier.createSupplier(start, end, regions, size);
        }

        private MemoryInfo getInfo(MemoryInfo[] from, long addr) {
            for (MemoryInfo info : from) {
                if (info.contains(addr)) {
                    return info;
                }
            }
            return null;
        }

        MemoryDump createDump(SearchResult res, DumpRegionSupplier supplier) throws IOException {

            if (NoexesFiles.getTempDir().getFreeSpace() < supplier.getSize()) {
                throw new IOException("Not enough free space for dump!");
            }
            boolean resume = conn.getStatus() == DebuggerStatus.PAUSED;

            //pause the game
            conn.pause();

            long totalSize = supplier.getSize();
            long lastUpdate = 0;
            long prevRead = 0;
            Rolling avg = new Rolling(10);
            long read = 0;
            MemoryDump dump;
            DumpOutputStream dout;
            try {
                dump = new MemoryDump(res.getLocation());
                dump.setTid(conn.getCurrentTitleId());
                dump.getInfos().addAll(Arrays.asList(conn.query(0,10000)));
                dout = dump.openStream();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            while (!isCancelled()) {
                DumpRegion r = supplier.get();
                if (r == null) {
                    break;
                }
                long size = r.getSize();
                long addr = r.getStart();
                while (size > 0 && !isCancelled()) {
                    if (System.currentTimeMillis() - lastUpdate > 500) {
                        avg.add(read - prevRead);
                        prevRead = read;
                        lastUpdate = System.currentTimeMillis();
                    }
                    int len = (int) Math.min(size, 2_000_000);
                    dout.setCurrentAddress(addr);
                    conn.readmem(addr, len, dout);
                    size -= len;
                    addr += len;
                    read += len;
                    double average = avg.getAverage() * 2;
                    long remaining = totalSize - read;
                    updateProgress(read, totalSize);
                    updateMessage(String.format("Dumping - DL: %s/s T: %s R: %s ETA: %s",
                            NetUtils.formatSize((long) (average)),
                            NetUtils.formatSize(totalSize),
                            NetUtils.formatSize(remaining),
                            TimeUtils.formatTime((long) (remaining / average * 1000))));

                }
            }
            if (dout != null) {
                dout.close();
            }
            if (resume) {
                conn.resume();
            }
            return isCancelled() ? null : dump;
        }

        private void addAddress(long addr) {
            if (addr < res.start) {
                res.start = addr;
            }
            if (addr > res.end) {
                res.end = addr;
            }
        }

    }
}
