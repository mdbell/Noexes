package me.mdbell.noexs.ui.models;

public enum DataType {
    BYTE("8 bit", 1),
    SHORT("16 bit", 2),
    INT("32 bit", 4),
    LONG("64 bit", 8);

    String str;
    int bytes;

    DataType(String str, int bytes) {
        this.str = str;
        this.bytes = bytes;
    }

    @Override
    public String toString() {
        return str;
    }

    public int getSize() {
        return bytes;
    }
}