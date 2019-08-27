package me.mdbell.noexs.ui.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import me.mdbell.javafx.control.AddressSpinner;
import me.mdbell.javafx.control.FormattedTableCell;
import me.mdbell.javafx.control.HexSpinner;
import me.mdbell.noexs.core.Debugger;
import me.mdbell.noexs.core.MemoryInfo;
import me.mdbell.noexs.ui.Settings;
import me.mdbell.noexs.ui.models.DataType;
import me.mdbell.noexs.ui.models.MemoryViewerTableModel;
import me.mdbell.util.HexUtils;
import me.mdbell.util.IPatternMatcher;
import me.mdbell.util.PatternCompiler;
import me.mdbell.util.PatternTokenizer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.TimerTask;
import java.util.function.Function;

public class MemoryViewerController implements IController {

    private static final int ROW_COUNT = 100;
    public TextField patternTextField;

    private MainController mainController;

    private long prevUpdateAddr;

    @FXML
    TableView<MemoryViewerTableModel> memViewTable;
    @FXML
    TableColumn<MemoryViewerTableModel, Number> memViewAddrCol;
    @FXML
    TableColumn<MemoryViewerTableModel, Number> memViewValCol1;
    @FXML
    TableColumn<MemoryViewerTableModel, Number> memViewValCol2;
    @FXML
    TableColumn<MemoryViewerTableModel, Number> memViewValCol3;
    @FXML
    TableColumn<MemoryViewerTableModel, Number> memViewValCol4;

    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii00;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii01;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii02;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii03;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii04;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii05;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii06;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii07;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii08;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii09;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii0A;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii0B;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii0C;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii0D;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii0E;
    @FXML
    TableColumn<MemoryViewerTableModel, String> ascii0F;

    @FXML
    CheckBox refreshCheckbox;

    @FXML
    AddressSpinner memViewAddrBox;

    @FXML
    ComboBox<DataType> pokeType;

    @FXML
    HexSpinner pokeValue;

    @FXML
    AnchorPane memViewTabPage;

    @FXML
    CheckBox endianCheckbox;

    private ObservableList<MemoryViewerTableModel> memoryList;
    private long lastAddress = 0;

    @FXML
    public void initialize() {
        //setup the memview table
        memViewTable.getSelectionModel().setCellSelectionEnabled(true);
        memoryList = FXCollections.observableArrayList();
        memViewTable.setItems(memoryList);

        for (TableColumn c : memViewTable.getColumns()) {
            c.setReorderable(false);
        }

        for (int i = 0; i < ROW_COUNT; i++) {
            memoryList.add(new MemoryViewerTableModel());
        }

        memViewAddrCol.setCellValueFactory(param -> param.getValue().addrProperty());

        memViewValCol1.setCellValueFactory(param -> param.getValue().firstValueProperty());
        memViewValCol2.setCellValueFactory(param -> param.getValue().secondValueProperty());
        memViewValCol3.setCellValueFactory(param -> param.getValue().thirdValueProperty());
        memViewValCol4.setCellValueFactory(param -> param.getValue().fourthValueProperty());
        ascii00.setCellValueFactory(param -> param.getValue().asciiBinding(0));
        ascii01.setCellValueFactory(param -> param.getValue().asciiBinding(1));
        ascii02.setCellValueFactory(param -> param.getValue().asciiBinding(2));
        ascii03.setCellValueFactory(param -> param.getValue().asciiBinding(3));
        ascii04.setCellValueFactory(param -> param.getValue().asciiBinding(4));
        ascii05.setCellValueFactory(param -> param.getValue().asciiBinding(5));
        ascii06.setCellValueFactory(param -> param.getValue().asciiBinding(6));
        ascii07.setCellValueFactory(param -> param.getValue().asciiBinding(7));
        ascii08.setCellValueFactory(param -> param.getValue().asciiBinding(8));
        ascii09.setCellValueFactory(param -> param.getValue().asciiBinding(9));
        ascii0A.setCellValueFactory(param -> param.getValue().asciiBinding(10));
        ascii0B.setCellValueFactory(param -> param.getValue().asciiBinding(11));
        ascii0C.setCellValueFactory(param -> param.getValue().asciiBinding(12));
        ascii0D.setCellValueFactory(param -> param.getValue().asciiBinding(13));
        ascii0E.setCellValueFactory(param -> param.getValue().asciiBinding(14));
        ascii0F.setCellValueFactory(param -> param.getValue().asciiBinding(15));

        Function<Number, String> addressFormatter = value -> HexUtils.formatAddress(value.longValue());
        Function<Number, String> valueFormatter = value -> HexUtils.formatInt(value.intValue());

        memViewAddrCol.setCellFactory(param -> new FormattedTableCell<>(addressFormatter));
        memViewValCol1.setCellFactory(param -> new FormattedTableCell<>(valueFormatter));
        memViewValCol2.setCellFactory(param -> new FormattedTableCell<>(valueFormatter));
        memViewValCol3.setCellFactory(param -> new FormattedTableCell<>(valueFormatter));
        memViewValCol4.setCellFactory(param -> new FormattedTableCell<>(valueFormatter));
        memViewTable.getFocusModel().focusedCellProperty().addListener((observable, oldValue, newValue) -> {
            long addr = getSelectedAddress();
            memViewAddrBox.getValueFactory().setValue(addr);
            if(addr != 0) {
                pokeValue.getValueFactory().setValue(mainController.getDebugger().peek(pokeType.getValue(), addr));
            }
        });

        pokeType.getItems().addAll(DataType.values());
        pokeType.setValue(DataType.INT);

        pokeType.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> pokeValue.setSize(newValue.getSize() * 2));

        endianCheckbox.selectedProperty().setValue(Settings.shouldSwapEndian());
        endianCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> dumpToMemoryViewer(lastAddress));
    }

    private long getSelectedAddress() {
        TablePosition pos = memViewTable.getFocusModel().getFocusedCell();
        long addr = lastAddress;
        if (addr == 0 || pos == null) {
            return 0;
        }
        int row = pos.getRow();
        int col = pos.getColumn();

        addr += row * 0x10;
        //is address selected
        if (col == 0) {
            return addr;
        }

        //is ascii selected
        if (col > 4) {
            return addr + (col - 5);
        }
        col--;
        return addr + (col * 4);
    }

    @Override
    public void stop() {
        Settings.setSwapEndian(endianCheckbox.isSelected());
    }

    public void dumpToMemoryViewer() {
        dumpToMemoryViewer(memViewAddrBox.getValue());
    }

    public void dumpToMemoryViewer(long updateAddr) {
        if (updateAddr == 0) {
            return;
        }

        boolean swap = endianCheckbox.isSelected();

        long base = updateAddr & ~0xF;
        lastAddress = base;
        Debugger conn = mainController.getDebugger();
        int size = ROW_COUNT * 16;
        ByteBuffer data = conn.readmem(base, size, new byte[size]);
        if(swap){
            data.order(ByteOrder.LITTLE_ENDIAN);
        }
        for (int i = 0; i < ROW_COUNT; i++) {
            long addr = base + i * 0x10;
            MemoryViewerTableModel m = memoryList.get(i);
            m.set(addr, data.getInt(), data.getInt(), data.getInt(), data.getInt());
        }
        mainController.runAndWait(() -> {
            memViewTable.refresh();
            if (updateAddr == prevUpdateAddr) {
                return;
            }
            prevUpdateAddr = updateAddr;
            long l = updateAddr;
            while (l % 4 != 0) {
                l--;
            }
            TableColumn<MemoryViewerTableModel, Number> column;
            switch ((int) ((l & 0xF) / 4)) {
                case 0:
                    column = memViewValCol1;
                    break;
                case 1:
                    column = memViewValCol2;
                    break;
                case 2:
                    column = memViewValCol3;
                    break;
                case 3:
                    column = memViewValCol4;
                    break;
                default:
                    return;
            }
            memViewTable.getSelectionModel().select(0, column);
        });
    }

    public void updateMemoryViewer(ActionEvent event) {
        dumpToMemoryViewer();
    }

    public void memViewerUpBig(ActionEvent event) {
        memViewAddrBox.decrement(ROW_COUNT * 0x10);
        dumpToMemoryViewer();
    }

    public void memViewerUp(ActionEvent event) {
        memViewAddrBox.decrement(0x10);
        dumpToMemoryViewer();
    }

    public void setViewAddress(long addr) {
        memViewAddrBox.getValueFactory().setValue(addr);
        dumpToMemoryViewer();
    }

    public void memViewerDownBig(ActionEvent event) {
        memViewAddrBox.increment(ROW_COUNT * 0x10);
        dumpToMemoryViewer();
    }

    public void memViewerDown(ActionEvent event) {
        memViewAddrBox.increment(0x10);
        dumpToMemoryViewer();
    }

    public void setMainController(MainController mc) {
        this.mainController = mc;
        mc.timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (refreshCheckbox.isSelected() && mc.getDebugger().connected()
                        && mc.getCurrentTab() == MainController.Tab.MEMORY_VIEWER) {
                    mc.runAndWait(() -> dumpToMemoryViewer(lastAddress));
                }
            }
        }, 1, 500);
    }

    @Override
    public void onConnect() {
        //memViewTabPage.setDisable(false);
    }

    @Override
    public void onDisconnect() {
        //memViewTabPage.setDisable(true);
    }

    public void populateMemory() {
        Debugger conn = mainController.getDebugger();
        if (!conn.connected()) {
            return;
        }
        //TODO not do this here.
        mainController.tools().updateMemoryInfo(conn.query(0, 10000));
    }

    PatternTokenizer tokenizer = new PatternTokenizer();
    PatternCompiler compiler = new PatternCompiler();

    public void onPokeAction(ActionEvent event) {
        Debugger debugger = mainController.getDebugger();
        debugger.poke(pokeType.getValue(), memViewAddrBox.getValue(), pokeValue.getValue());
        dumpToMemoryViewer(lastAddress);
    }

    public void onPatternSearch(ActionEvent event) {
        long address = memViewAddrBox.getValue();
        if(address == 0) {
            //TODO say invalid address
            return;
        }
        String str = patternTextField.getText().trim();
        if(str.length() == 0) {
            //TODO say invalid pattern
            return;
        }
        PatternCompiler.PatternElement[] elements = tokenizer.eval(str);

        IPatternMatcher matcher = compiler.compile(elements);

        //TODO do this in a service, not here
        MemoryInfo info = mainController.getDebugger().query(address);
        int size = (int) (mainController.getDebugger().query(address).getSize() - (address - info.getAddress()));
        byte[] bytes = new byte[size];
        ByteBuffer buffer = mainController.getDebugger().readmem(address, size, bytes);
        int idx = matcher.match(buffer);
        if(idx != -1) {
            memViewAddrBox.getValueFactory().setValue(address + idx);
            dumpToMemoryViewer();
        }
    }
}
