package me.mdbell.noexs.dump;

public class DumpRegionFactory {

    private long start;
    private long end;

    private DumpRegionFactory(){

    }

    public DumpRegionFactory setLength(long len) {
        end = start + len;
        return this;
    }

    public DumpRegionFactory setStart(long start){
        this.start = start;
        return this;
    }

    public DumpRegionFactory setEnd(long end){
        this.end = end;
        return this;
    }

    public static DumpRegionFactory create(){
        return new DumpRegionFactory();
    }

    public long getEnd(){
        return end;
    }

    public long getStart(){
        return start;
    }

    public long getLength(){
        return end - start;
    }

    public DumpRegion build(){
        return new DumpRegion(start, end);
    }
}
