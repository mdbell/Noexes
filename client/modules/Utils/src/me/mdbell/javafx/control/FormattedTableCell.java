package me.mdbell.javafx.control;

import javafx.scene.control.TableCell;

import java.util.function.Function;

public class FormattedTableCell<S, T> extends TableCell<S, T> {

    private final Function<T, String> formatter;
    private final String defaultValue;

    public FormattedTableCell(Function<T, String> formatter) {
        this(formatter, "");
    }

    public FormattedTableCell(Function<T, String> formatter, String defaultValue) {
        this.formatter = formatter;
        this.defaultValue = defaultValue;
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if(empty || item == null) {
            setText(defaultValue);
        }else{
            setText(formatter.apply(item));
        }
    }
}
