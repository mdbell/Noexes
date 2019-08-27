package me.mdbell.javafx.control;

import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

public class SpinnerTableCell<S, V extends Spinner<T>, T> extends TableCell<S, T> {

    private V spinner;

    public SpinnerTableCell(TableColumn<S, T> col, V spinner) {
        this.spinner = spinner;
        this.spinner.valueProperty().addListener((observable, oldValue, newValue) -> commitEdit(newValue));
        this.spinner.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                getTableView().edit(getIndex(), getTableColumn());
            }
        });
        col.editableProperty().bind(spinner.editableProperty());
    }

    public V getSpinner(){
        return spinner;
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
            return;
        }

        if(item != null) {
            this.spinner.getValueFactory().setValue(item);
        }
        setGraphic(spinner);
    }
}
