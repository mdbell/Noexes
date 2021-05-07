package me.mdbell.noexs.ui.models;

import me.mdbell.util.ILocalized;

public enum ConnectionType implements ILocalized {

    USB("main.conn.usb"), NETWORK("main.conn.network");

    private String key;

    ConnectionType(String key){
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }
}
