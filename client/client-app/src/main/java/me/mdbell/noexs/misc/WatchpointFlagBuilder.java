package me.mdbell.noexs.misc;

public class WatchpointFlagBuilder {

    boolean enabled;
    MatchType lsc = MatchType.ALL;
    int bas;
    int lbn;
    int mask;

    public WatchpointFlagBuilder setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public WatchpointFlagBuilder setAddressSelect(int bas) {
        this.bas = bas;
        return this;
    }

    public WatchpointFlagBuilder setAccessContol(MatchType type) {
        this.lsc = type;
        return this;
    }

    public WatchpointFlagBuilder setLinkedBreakpointNumber(int number){
        this.lbn = number;
        return this;
    }

    public WatchpointFlagBuilder setAddressMask(int mask) {
        this.mask = mask;
        return this;
    }

    public long getFlag(){
        long res = 0;

        if(enabled) {
            res |= 0x1;
        }

        res |= lsc.value << 3;

        res |= bas << 5;
        res |= lbn << 16;
        res |= mask << 24;

        return res;
    }

    public enum MatchType{
        LOAD(0b01),
        STORE(0b10),
        ALL(0b11);
        int value;

        MatchType(int value) {
            this.value = value;
        }
    }
}
