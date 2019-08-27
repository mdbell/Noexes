package me.mdbell.noexs.misc;

public enum BreakpointType{
    UNLINKED_INSN_ADDRESS_MATCH(0b0000),
    LINKED_INSN_ADDRESS_MATCH(0b0001),
    UNLINKED_CONTEXT_IDR_MATCH(0b0010),
    LINKED_CONTEXT_IDR_MATCH(0b0011),
    UNLINKED_ISNS_ADDRESS_MISMATCH(0b0100),
    LINKED_INSN_ADDRESS_MISMATCH(0b0101),
    UNLINKED_VMID_MATCH(0b1000),
    LINKED_VMID_MATCH(0b1001),
    UNLINKED_VMID_CONTEXT_IDR_MATCH(0b1010),
    LINKED_VMID_CONTEXT_IDR_MATCH(0b1011);
    int value;
    BreakpointType(int value){
        this.value = value;
    }

    int getValue(){
        return value;
    }
}
