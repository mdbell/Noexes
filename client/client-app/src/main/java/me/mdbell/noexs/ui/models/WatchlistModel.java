package me.mdbell.noexs.ui.models;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import me.mdbell.util.HexUtils;

public class WatchlistModel {

    SimpleBooleanProperty update = new SimpleBooleanProperty();
    SimpleBooleanProperty locked = new SimpleBooleanProperty();
    SimpleStringProperty desc = new SimpleStringProperty("-");
    SimpleObjectProperty<String> addr = new SimpleObjectProperty<>(HexUtils.formatAddress(0));
    SimpleObjectProperty<Long> value = new SimpleObjectProperty<>(0L);
    SimpleObjectProperty<DataType> type = new SimpleObjectProperty<>(DataType.INT);

    public WatchlistModel() {
        updateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                lockedProperty().setValue(false);
            }
        });
        lockedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                updateProperty().setValue(false);
            }
        });
    }

    public SimpleBooleanProperty updateProperty() {
        return update;
    }

    public SimpleBooleanProperty lockedProperty() {
        return locked;
    }

    public SimpleStringProperty descProperty() {
        return desc;
    }

    public SimpleObjectProperty<String> addrProperty() {
        return addr;
    }

    public SimpleObjectProperty<Long> valueProperty() {
        return value;
    }

    public SimpleObjectProperty<DataType> typeProperty() {
        return type;
    }

    public boolean canUpdate() {
        return updateProperty().get();
    }

    public void setUpdate(boolean update) {
        this.update.setValue(update);
    }

    public boolean isLocked() {
        return locked.get();
    }

    public void setLocked(boolean locked) {
        this.locked.set(locked);
    }

    public DataType getType() {
        return type.get();
    }

    public void setType(DataType t) {
        type.set(t);
    }

    public void setAddr(String addr) {
        this.addr.set(addr);
    }

    public String getAddr() {
        return addr.get();
    }

    public void setDesc(String desc) {
        this.desc.set(desc);
    }

    public String getDesc() {
        return desc.get();
    }

    public long getValue() {
        return value.get();
    }

    public void setValue(long value) {
        this.value.set(value);
    }

    @Override
    public String toString() {
        return "WatchlistModel{" +
                "locked=" + locked +
                ", desc=" + desc +
                ", addr=" + addr +
                ", value=" + value +
                ", type=" + type +
                '}';
    }

    public int getSize() {
        return type.get().getSize();
    }
}
