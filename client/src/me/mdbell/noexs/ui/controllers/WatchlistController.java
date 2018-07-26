package me.mdbell.noexs.ui.controllers;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;
import me.mdbell.javafx.control.AddressSpinner;
import me.mdbell.javafx.control.HexSpinner;
import me.mdbell.javafx.control.SpinnerTableCell;
import me.mdbell.noexs.core.Debugger;
import me.mdbell.noexs.ui.models.DataType;
import me.mdbell.noexs.ui.models.WatchlistModel;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

public class WatchlistController implements IController {

    public Button addButton;
    public Button removeButton;
    public TableView<WatchlistModel> watchlistTable;
    public TableColumn<WatchlistModel, Boolean> updateCol;
    public TableColumn<WatchlistModel, Boolean> lockedCol;
    public TableColumn<WatchlistModel, Long> addrCol;
    public TableColumn<WatchlistModel, String> descCol;
    public TableColumn<WatchlistModel, DataType> typeCol;
    public TableColumn<WatchlistModel, Long> valueCol;
    @FXML
    AnchorPane watchlistTabPage;

    private MainController mc;

    private Semaphore semaphore = new Semaphore(1);

    @FXML
    public void initialize() {
        updateCol.setCellValueFactory(param -> param.getValue().updateProperty());
        lockedCol.setCellValueFactory(param -> param.getValue().lockedProperty());
        typeCol.setCellValueFactory(param -> param.getValue().typeProperty());
        addrCol.setCellValueFactory(param -> param.getValue().addrProperty());
        descCol.setCellValueFactory(param -> param.getValue().descProperty());
        valueCol.setCellValueFactory(param -> param.getValue().valueProperty());

        updateCol.setCellFactory(param -> new CheckBoxTableCell<>());
        lockedCol.setCellFactory(param -> new CheckBoxTableCell<>());
        typeCol.setCellFactory(param -> new ComboBoxTableCell<>(DataType.values()) {
            {
                setItem(DataType.INT);
            }
        });
        addrCol.setCellFactory(param -> new SpinnerTableCell<>(addrCol, new AddressSpinner() {
            {
                setEditable(true);
            }
        }));
        descCol.setCellFactory(TextFieldTableCell.forTableColumn());
        valueCol.setCellFactory(param -> new SpinnerTableCell<>(valueCol, new HexSpinner() {
                    {
                        setEditable(true);
                        setSize(16);
                    }
                }) {
                    @Override
                    protected void updateItem(Long item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            return;
                        }
                        WatchlistModel model = getTableRow().getItem();
                        if (model == null) {
                            return;
                        }
                        getSpinner().setSize(model.getSize() * 2);
                    }
                }
        );

        watchlistTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> removeButton.setDisable(newValue == null));
    }

    @Override
    public void setMainController(MainController c) {
        this.mc = c;
        mc.timer().scheduleAtFixedRate(new WatchlistTimerTask(), 1, 100);
    }

    @Override
    public void onConnect() {
        watchlistTabPage.setDisable(false);
    }

    @Override
    public void onDisconnect() {
        //watchlistTabPage.setDisable(true);
    }

    public void addAddr(long addr) {
        WatchlistModel model = new WatchlistModel();
        model.setAddr(addr);
        model.setUpdate(true);
        update(model);
    }

    private void update(WatchlistModel model) {
        List<WatchlistModel> modelList = watchlistTable.getItems();
        acquire();
        modelList.add(model);
        release();
        model.typeProperty().addListener((observable, oldValue, newValue) -> watchlistTable.refresh());
    }

    public void onAddAction(ActionEvent event) {
        update(new WatchlistModel());
    }

    public void onRemoveAction(ActionEvent event) {
        WatchlistModel model = watchlistTable.getSelectionModel().getSelectedItem();
        acquire();
        watchlistTable.getItems().remove(model);
        release();
    }

    private void acquire() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void release() {
        semaphore.release();
    }

    private class WatchlistTimerTask extends TimerTask {

        @Override
        public void run() {
            Debugger debugger = mc.getConnection();
            if (!debugger.connected()) {
                return;
            }
            acquire();
            try {
                for (WatchlistModel m : watchlistTable.getItems()) {
                    if (m.getAddr() == 0) {
                        continue;
                    }
                    if (m.canUpdate()) {
                        long value = mc.getConnection().peek(m.getType(), m.getAddr());
                        if (m.getValue() != value) {
                            m.setValue(value);
                        }
                    } else if (m.isLocked()) {
                        mc.getConnection().poke(m.getType(), m.getAddr(), m.getValue());
                    }
                }
            } catch (Exception ignored) {

            } finally {
                release();
            }
        }
    }
}
