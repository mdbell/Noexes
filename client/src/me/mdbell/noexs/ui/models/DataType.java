package me.mdbell.noexs.ui.models;

import me.mdbell.util.ILocalized;

public enum DataType implements ILocalized {
    BYTE("search.data_types.byte", 1),
    SHORT("search.data_types.short", 2),
    INT("search.data_types.int", 4),
    LONG("search.data_types.long", 8);

    String key;
    int bytes;

    DataType(String key, int bytes) {
        this.key = key;
        this.bytes = bytes;
    }

    @Override
    public String getKey() {
        return key;
    }

    public int getSize() {
        return bytes;
    }
}