package me.mdbell.noexs.ui.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;
import me.mdbell.javafx.control.HexSpinner;
import me.mdbell.javafx.control.SpinnerTableCell;
import me.mdbell.noexs.core.Debugger;
import me.mdbell.noexs.ui.models.DataType;
import me.mdbell.noexs.ui.models.WatchlistModel;
import me.mdbell.util.HexUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

public class WatchlistController implements IController {

    public Button addButton;
    public Button removeButton;
    public TableView<WatchlistModel> watchlistTable;
    public TableColumn<WatchlistModel, Boolean> updateCol;
    public TableColumn<WatchlistModel, Boolean> lockedCol;
    public TableColumn<WatchlistModel, String> addrCol;
    public TableColumn<WatchlistModel, String> descCol;
    public TableColumn<WatchlistModel, DataType> typeCol;
    public TableColumn<WatchlistModel, Long> valueCol;
    @FXML
    AnchorPane watchlistTabPage;

    private MainController mc;

    private Semaphore semaphore = new Semaphore(1);
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();


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
        addrCol.setCellFactory(TextFieldTableCell.forTableColumn());
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
        setAddr(model, addr);
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

    public void clearList(ActionEvent event) {
        watchlistTable.getItems().clear();
    }

    static class SerializedWatchlistItem {
        boolean update;
        boolean locked;
        String addr;
        String desc;
        DataType type;
        long value;

        protected SerializedWatchlistItem() {

        }
    }

    public void onSave(ActionEvent event) {
        File f = mc.browseFile(true, null, "Save As...", "Watchlist File", "*.json");
        if (f == null) {
            return;
        }
        List<SerializedWatchlistItem> list = new LinkedList<>();
        watchlistTable.getItems().forEach(item -> {
            SerializedWatchlistItem sw = new SerializedWatchlistItem();
            sw.update = item.canUpdate();
            sw.locked = item.isLocked();
            sw.addr = item.getAddr();
            sw.desc = item.getDesc();
            sw.type = item.getType();
            sw.value = item.getValue();
            list.add(sw);
        });
        String json = gson.toJson(list);
        try {
            Files.write(f.toPath(), json.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onLoad(ActionEvent event) {
        File f = mc.browseFile(false, null, "Open...", "Watchlist File", "*.json");
        if(f == null){
            return;
        }

        watchlistTable.getItems().clear();
        Type listOfTestObject = new TypeToken<List<SerializedWatchlistItem>>(){}.getType();
        try {
            FileReader reader = new FileReader(f);
            List<SerializedWatchlistItem> list = gson.fromJson(reader, listOfTestObject);
            list.forEach( item ->{
                WatchlistModel model = new WatchlistModel();
                model.setUpdate(item.update);
                model.setLocked(item.locked);
                model.setAddr(item.addr);
                model.setDesc(item.desc);
                model.setType(item.type);
                model.setValue(item.value);
                watchlistTable.getItems().add(model);
            });

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private class WatchlistTimerTask extends TimerTask {

        @Override
        public void run() {
            Debugger debugger = mc.getDebugger();
            if (!debugger.connected()) {
                return;
            }
            acquire();
            try {
                for (WatchlistModel m : watchlistTable.getItems()) {
                    if (getAddr(m) == 0) {
                        continue;
                    }
                    if (m.canUpdate()) {
                        long value = mc.getDebugger().peek(m.getType(), getAddr(m));
                        if (m.getValue() != value) {
                            m.setValue(value);
                        }
                    } else if (m.isLocked()) {
                        mc.getDebugger().poke(m.getType(), getAddr(m), m.getValue());
                    }
                }
            } catch (Exception ignored) {
            } finally {
                release();
            }
        }
    }

    private void setAddr(WatchlistModel m, long addr) {
        m.setAddr(HexUtils.formatAddress(addr));
    }

    private long getAddr(WatchlistModel m) {
        return mc.tools().getEvaluator().eval(m.getAddr());
    }
}
