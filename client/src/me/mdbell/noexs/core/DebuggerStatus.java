package me.mdbell.noexs.core;

public enum DebuggerStatus {
    STOPPED(0, "Shutting down"), RUNNING(1, "Running"), PAUSED(2, "Paused");

    int id;
    String str;

    DebuggerStatus(int id, String str) {
        this.id = id;
        this.str = str;
    }

    public int getId() {
        return id;
    }

    public String getStatus() {
        return str;
    }

    public static DebuggerStatus forId(int status) {
        for (DebuggerStatus s : values()) {
            if (s.id == status) {
                return s;
            }
        }
        return null;
    }
}
