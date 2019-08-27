package me.mdbell.javafx.control;

import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;
import me.mdbell.util.HexUtils;

public class HexSpinner extends Spinner<Long> {

    private int size;

    public HexSpinner(){
        setValueFactory(new HexValueFactory());
        setSize(8);
    }

    public void setSize(int len) {
        this.size = len;
        getEditor().setText(getValueAsString());
    }

    public String getValueAsString(){
        return getValueFactory().getConverter().toString(getValue());
    }

    class HexValueFactory extends SpinnerValueFactory<Long> {

        public HexValueFactory() {
            setValue(0L);
            valueProperty().addListener((observable, oldValue, newValue) -> {
                setValue(newValue);
            });
            setConverter(new StringConverter<Long>() {
                @Override
                public String toString(Long value) {
                    return HexUtils.pad('0', size, Long.toUnsignedString(value, 16).toUpperCase());
                }

                @Override
                public Long fromString(String string) {
                    try {
                        return Long.parseUnsignedLong(string.trim(), 16);
                    } catch (NumberFormatException e) {
                        return getValue();
                    }
                }
            });


        }

        @Override
        public void decrement(int steps) {
            long l = getValue();
            l -= steps;
            setValue(l);
        }

        @Override
        public void increment(int steps) {
            long l = getValue();
            l += steps;
            setValue(l);
        }
    }
}
