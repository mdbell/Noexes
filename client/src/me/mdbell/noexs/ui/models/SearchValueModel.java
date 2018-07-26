package me.mdbell.noexs.ui.models;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableLongValue;
import javafx.beans.value.ObservableNumberValue;
import javafx.collections.ObservableList;
import me.mdbell.noexs.ui.services.MemorySearchService;
import me.mdbell.util.HexUtils;

import java.io.IOException;

public class SearchValueModel {

    private SimpleLongProperty addr;
    private SimpleLongProperty oldValue;
    private SimpleLongProperty newValue;
    private ObservableNumberValue diff;


    public SearchValueModel(long addr, long prev, long current) {
        init(addr, prev, current);
    }

    private void init(long addr, long prev, long current) {
        this.addr = new SimpleLongProperty(addr);
        this.oldValue = new SimpleLongProperty(prev);
        this.newValue = new SimpleLongProperty(current);
        this.diff = Bindings.subtract(newValue, oldValue);
    }

    public SimpleLongProperty addrProperty() {
        return addr;
    }

    public SimpleLongProperty oldValueProperty() {
        return oldValue;
    }

    public SimpleLongProperty newValueProperty() {
        return newValue;
    }

    public ObservableNumberValue diffProperty() {
        return diff;
    }

    public long getAddr() {
        return addr.get();
    }

    public long getOldValue() {
        return oldValue.get();
    }

    public long getNewValue() {
        return newValue.get();
    }
}
