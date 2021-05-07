package me.mdbell.util;

import javafx.util.StringConverter;

import java.util.ResourceBundle;
import java.util.function.Supplier;

public class LocalizedStringConverter<T extends ILocalized> extends StringConverter<T> {

    private final Supplier<ResourceBundle> supplier;

    public LocalizedStringConverter(Supplier<ResourceBundle> supplier){
        this.supplier = supplier;
    }

    @Override
    public String toString(T t) {
        ResourceBundle bundle = supplier.get();
        String key = t.getKey();
        if(bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        return t.toString();
    }

    @Override
    public T fromString(String s) {
        return null;
    }
}
