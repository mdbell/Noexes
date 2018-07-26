package me.mdbell.noexs.ui.models;

public enum SearchType {
    UNKNOWN("Unknown"), PREVIOUS("Previous"), KNOWN("Known"), DIFFERENT("Different");
    String readable;

    SearchType(String str){
        this.readable = str;
    }

    public String toString(){
        return readable;
    }

    public boolean requiresPrevious() {
        return this == PREVIOUS || this == DIFFERENT;
    }
}
