package me.mdbell.noexs.ui.models;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import me.mdbell.noexs.core.MemoryInfo;
import me.mdbell.noexs.core.MemoryType;
import me.mdbell.util.HexUtils;

public class MemoryInfoTableModel {

    private SimpleStringProperty name;
    private SimpleLongProperty addr;
    private SimpleLongProperty size;
    private NumberBinding end;
    private SimpleObjectProperty<MemoryType> type;
    private SimpleIntegerProperty access;

    public MemoryInfoTableModel(MemoryInfo info) {
        this("-", info);
    }

    public MemoryInfoTableModel(String name, MemoryInfo info) {
        this.name = new SimpleStringProperty(name);
        this.addr = new SimpleLongProperty(info.getAddress());
        this.size = new SimpleLongProperty(info.getSize());
        this.end = Bindings.add(addr, size);
        this.type = new SimpleObjectProperty<>(info.getType());
        this.access = new SimpleIntegerProperty(info.getPerm());
    }

    public SimpleStringProperty nameProperty(){
        return name;
    }

    public SimpleLongProperty addrProperty(){
        return addr;
    }

    public long getAddr(){
        return addr.get();
    }

    public SimpleLongProperty sizeProperty(){
        return size;
    }

    public NumberBinding endProperty(){
        return end;
    }

    public SimpleObjectProperty<MemoryType> typeProperty(){
        return type;
    }

    public SimpleIntegerProperty accessProperty(){
        return access;
    }

    public long getEnd() {
        return end.longValue();
    }
}
