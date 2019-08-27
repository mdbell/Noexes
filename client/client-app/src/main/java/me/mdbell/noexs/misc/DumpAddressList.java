package me.mdbell.noexs.misc;

import me.mdbell.noexs.dump.DumpIndex;
import me.mdbell.noexs.dump.MemoryDump;
import me.mdbell.util.HexUtils;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class DumpAddressList extends AbstractList<Long> {

    private List<OffsetList> indexList = new ArrayList<>();

    public DumpAddressList(MemoryDump dump, int dataSize) {
        for (DumpIndex idx : dump.getIndices()) {
            long start = idx.getAddress();
            int size = (int) (idx.getEndAddress() - start) / dataSize;
            OffsetList lst = new OffsetList(start, size, dataSize);
            indexList.add(lst);
        }
    }

    @Override
    public Long get(int index) {
        for (OffsetList list : indexList) {
            int size = list.size();
            if (index < size) {
                return list.get(index);
            }
            index -= size;
        }
        return null;
    }

    @Override
    public int size() {
        int size = 0;
        for (OffsetList list : indexList) {
            size += list.size();
        }
        return size;
    }
}
