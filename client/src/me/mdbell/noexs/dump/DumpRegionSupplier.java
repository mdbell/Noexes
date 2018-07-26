package me.mdbell.noexs.dump;

import me.mdbell.noexs.core.Debugger;
import me.mdbell.noexs.core.MemoryInfo;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class DumpRegionSupplier implements Supplier<DumpRegion> {

    public abstract long getStart();

    public abstract long getEnd();

    public long getSize() {
        return getEnd() - getStart();
    }

    public static DumpRegionSupplier createSupplier(long start, long end, List<DumpRegion> regions) {
        return createSupplier(start, end, regions, end - start);
    }

    public static DumpRegionSupplier createSupplier(long start, long end, List<DumpRegion> regions, long size) {
        return new DumpRegionSupplier() {
            int i = 0;

            @Override
            public long getStart() {
                return start;
            }

            @Override
            public long getEnd() {
                return end;
            }

            @Override
            public long getSize() {
                return size;
            }

            @Override
            public DumpRegion get() {
                if (i >= regions.size()) {
                    return null;
                }
                return regions.get(i++);
            }
        };
    }

    public static DumpRegionSupplier createSupplierFromInfo(Debugger conn, Function<MemoryInfo, Boolean> filter) {
        return new DumpRegionSupplier() {
            long start = 0;
            long end = 0;
            long size;

            @Override
            public long getStart() {
                init();
                return start;
            }

            @Override
            public long getEnd() {
                init();
                return end;
            }

            @Override
            public long getSize() {
                init();
                return size;
            }

            MemoryInfo[] info;
            int i = 0;

            private void init() {
                if (info == null) {
                    info = conn.query(0, 10000);
                    for (int i = 0; i < info.length; i++) {
                        MemoryInfo in = info[i];
                        if (!filter.apply(in)) {
                            continue;
                        }
                        long addr = in.getAddress();
                        long next = in.getNextAddress();
                        if (start == 0) {
                            start = addr;
                        }
                        if (next > end) {
                            end = next;
                        }
                        size += in.getSize();
                    }
                }
            }

            @Override
            public DumpRegion get() {
                init();
                MemoryInfo curr;
                do {
                    if (i >= info.length) {
                        return null;
                    }
                    curr = info[i++];
                } while (!filter.apply(curr));
                return new DumpRegion(curr.getAddress(), curr.getNextAddress());
            }
        };
    }

    public static DumpRegionSupplier createSupplierFromRange(Debugger conn, long start, long end) {
        return new DumpRegionSupplier() {
            @Override
            public long getStart() {
                return start;
            }

            @Override
            public long getEnd() {
                return end;
            }

            MemoryInfo[] info;
            int i = 0;

            @Override
            public DumpRegion get() {
                if (info == null) {
                    info = conn.query(start, 10000);
                }
                MemoryInfo curr;
                do {
                    if (i >= info.length) {
                        return null;
                    }
                    curr = info[i++];
                } while (!curr.isReadable() || curr.getNextAddress() < start);
                if (curr.getAddress() >= end) {
                    return null;
                }
                return new DumpRegion(curr.getAddress(), Math.min(curr.getNextAddress(), end));
            }
        };
    }
}
