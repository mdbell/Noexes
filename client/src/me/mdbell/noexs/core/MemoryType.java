package me.mdbell.noexs.core;

public enum MemoryType {
    UNMAPPED(0),
    IO(0x01),
    NORMAL(0x02),
    CODE_STATIC(0x03),
    CODE_MUTABLE(0x04),
    HEAP(0x05),
    SHARED(0x06),
    WEIRD_MAPPED(0x07),
    MODULE_CODE_STATIC(0x08),
    MODULE_CODE_MUTABLE(0x09),
    IPC_BUFFER_0(0x0A),
    MAPPED(0xB),
    THREAD_LOCAL(0x0C),
    ISOLATED_TRANSFER(0x0D),
    TRANSFER(0x0E),
    PROCESS(0x0F),
    RESERVED(0x10),
    IPC_BUFFER_1(0x11),
    IPB_BUFFER_3(0x12),
    KERNEL_STACH(0x13),
    CODE_READ_ONLY(0x14),
    CODE_WRITABLE(0x15);

    int type;
    MemoryType(int type){
        this.type = type;
    }

    public int getType(){
        return type;
    }

    public static MemoryType valueof(int i) {
        for(MemoryType t : values()) {
            if(t.getType() == i) {
                return t;
            }
        }
        return null;
    }
}
