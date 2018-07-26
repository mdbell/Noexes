package me.mdbell.noexs.ui.models;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import me.mdbell.util.HexUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MemoryViewerTableModel {
    private final SimpleLongProperty addr;
    private final SimpleLongProperty value1;
    private final SimpleLongProperty value2;
    private final SimpleLongProperty value3;
    private final SimpleLongProperty value4;
    private final StringBinding[] asciiBindings = new StringBinding[16];

    public MemoryViewerTableModel() {
        this.addr = new SimpleLongProperty();
        this.value1 = new SimpleLongProperty();
        this.value2 = new SimpleLongProperty();
        this.value3 = new SimpleLongProperty();
        this.value4 = new SimpleLongProperty();
        for(int i = 0; i < asciiBindings.length; i++) {
            asciiBindings[i] = createBinding(i);
        }
        set(0,0,0,0,0);
    }

    private StringBinding createBinding(int i) {
        SimpleLongProperty prop;
        switch(i / 4){
            case 0:
                prop = value1;
                break;
            case 1:
                prop = value2;
                break;
            case 2:
                prop = value3;
                break;
            case 3:
                prop = value4;
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(i));
        }
        int rem = i % 4;
        return Bindings.createStringBinding(() -> {
            int j = (prop.intValue() >> (24 - rem * 8)) & 0xFF;
            return new String(new byte[]{formatChar(j)}, StandardCharsets.UTF_8);
        }, prop);
    }

    private byte formatChar(int i) {
        if(i < 0x20 || i > 0x7E && i < 0xA1) {
            return '.';
        }
        return (byte)i;
    }

    public void set(long addr, int memVal1, int memVal2, int memVal3, int memVal4) {
        this.addr.set(addr);
        this.value1.set(memVal1);
        this.value2.set(memVal2);
        this.value3.set(memVal3);
        this.value4.set(memVal4);
    }

    public SimpleLongProperty addrProperty(){
        return addr;
    }

    public SimpleLongProperty firstValueProperty(){
        return value1;
    }

    public SimpleLongProperty secondValueProperty(){
        return value2;
    }

    public SimpleLongProperty thirdValueProperty(){
        return value3;
    }

    public SimpleLongProperty fourthValueProperty(){
        return value4;
    }

    public StringBinding asciiBinding(int i){
        return asciiBindings[i];
    }
}