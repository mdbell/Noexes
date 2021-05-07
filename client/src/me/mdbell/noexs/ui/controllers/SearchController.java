package me.mdbell.noexs.ui.controllers;

import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import me.mdbell.javafx.control.AddressSpinner;
import me.mdbell.javafx.control.FormattedLabel;
import me.mdbell.javafx.control.FormattedTableCell;
import me.mdbell.javafx.control.HexSpinner;
import me.mdbell.noexs.core.Debugger;
import me.mdbell.noexs.core.MemoryType;
import me.mdbell.noexs.dump.DumpRegionSupplier;
import me.mdbell.noexs.ui.models.*;
import me.mdbell.noexs.ui.services.MemorySearchService;
import me.mdbell.noexs.ui.services.SearchResult;
import me.mdbell.util.HexUtils;
import me.mdbell.util.LocalizedStringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;

public class SearchController implements IController {

    private MainController mc;

    @FXML
    ComboBox<RangeType> searchType;

    @FXML
    ComboBox<DataType> dataTypeDropdown;

    @FXML
    ComboBox<SearchType> searchConditionTypeDropdown;
    @FXML
    ComboBox<ConditionType> searchConditionDropdown;

    @FXML
    AddressSpinner searchStart;

    @FXML
    AddressSpinner searchEnd;

    @FXML
    HexSpinner knownValue;

    @FXML
    AnchorPane searchTabPage;

    @FXML
    TableView<SearchValueModel> searchResults;

    @FXML
    TableColumn<SearchValueModel, Number> searchAddr;
    @FXML
    TableColumn<SearchValueModel, Number> oldValue;
    @FXML
    TableColumn<SearchValueModel, Number> newValue;
    @FXML
    TableColumn<SearchValueModel, Number> diff;

    @FXML
    HexSpinner pokeValue;

    @FXML
    TitledPane searchOptions;

    @FXML
    FormattedLabel conditionLabel;

    @FXML
    Button pageLeft;

    @FXML
    Button pageRight;

    @FXML
    FormattedLabel pageLabel;

    private SearchResult result;

    private Service<?> runningService = null;
    private final MemorySearchService searchService = new MemorySearchService();

    private ObservableList<SearchValueModel> resultList;

    private int currentPage = 0;

    private ResourceBundle bundle;

    @Override
    public void initialize(URL url, ResourceBundle bundle) {
        this.bundle = bundle;
        pageLabel.setFormattedText(0, 0, 0);

        searchType.setConverter(new LocalizedStringConverter<>(() -> bundle));
        searchType.getItems().addAll(RangeType.values());

        searchType.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            boolean b = newValue != RangeType.RANGE;
            searchStart.setDisable(b);
            searchEnd.setDisable(b);
        });
        searchType.getSelectionModel().select(RangeType.ALL);

        dataTypeDropdown.setConverter(new LocalizedStringConverter<>(() -> bundle));
        dataTypeDropdown.getItems().addAll(DataType.values());
        dataTypeDropdown.getSelectionModel().select(DataType.INT); // Default is 32-bit

        dataTypeDropdown.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            int size = newValue.getSize() * 2;
            knownValue.setSize(size);
            pokeValue.setSize(size);
        });

        searchConditionTypeDropdown.setConverter(new LocalizedStringConverter<>(() -> bundle));
        searchConditionTypeDropdown.getItems().addAll(SearchType.values());
        searchConditionTypeDropdown.getSelectionModel().select(SearchType.KNOWN); // Default is SPEC Value

        searchConditionTypeDropdown.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    boolean b = newValue == SearchType.UNKNOWN;
                    knownValue.setDisable(b);
                    searchConditionDropdown.setDisable(b);
                    if (b) {
                        searchConditionDropdown.setValue(ConditionType.EQUALS);
                    }
                    updateCondition();
                });

        searchConditionDropdown.setConverter(new LocalizedStringConverter<>(() -> bundle));
        searchConditionDropdown.getItems().addAll(ConditionType.values());
        searchConditionDropdown.getSelectionModel().select(ConditionType.EQUALS);

        searchConditionDropdown.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateCondition());


        knownValue.valueProperty().addListener((observable, oldValue, newValue) -> updateCondition());
        knownValue.getEditor().textProperty().addListener((observable, oldValue, newValue) -> updateCondition());

        resultList = FXCollections.observableArrayList();

        searchResults.setItems(resultList);
        searchResults.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        searchResults.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                pokeValue.getValueFactory().setValue(newValue.getNewValue());
            }
        });

        Function<Number, String> valueFormatter = value ->
                HexUtils.pad('0', result.getDataType().getSize() * 2, Long.toUnsignedString(value.longValue(), 16));

        searchAddr.setCellFactory(param -> new FormattedTableCell<>(value -> HexUtils.formatAddress(value.longValue())));
        oldValue.setCellFactory(param -> new FormattedTableCell<>(valueFormatter));
        newValue.setCellFactory(param -> new FormattedTableCell<>(valueFormatter));
        diff.setCellFactory(param -> new FormattedTableCell<>(value -> {
            long l = value.longValue();
            return (l < 0 ? "-" : "") + valueFormatter.apply(Math.abs(l));
        }));

        searchAddr.setCellValueFactory(param -> param.getValue().addrProperty());
        oldValue.setCellValueFactory(param -> param.getValue().oldValueProperty());
        newValue.setCellValueFactory(param -> param.getValue().newValueProperty());
        diff.setCellValueFactory(param -> param.getValue().diffProperty());

        ContextMenu cm = new ContextMenu();

        MenuItem memoryView = new MenuItem(bundle.getString("main.tabs.memory"));
        memoryView.setOnAction(event -> {
            SearchValueModel m = searchResults.getSelectionModel().getSelectedItem();
            if (m == null) {
                return;
            }
            mc.memory().setViewAddress(m.getAddr());
            mc.setTab(MainController.Tab.MEMORY_VIEWER);
        });
        MenuItem watchList = new MenuItem(bundle.getString("main.tabs.watchlist"));
        watchList.setOnAction(event -> {
            SearchValueModel m = searchResults.getSelectionModel().getSelectedItem();
            if (m == null) {
                return;
            }
            mc.watch().addAddr(m.getAddr());
            mc.setTab(MainController.Tab.WATCH_LIST);
        });
        cm.getItems().addAll(memoryView, watchList);
        searchResults.contextMenuProperty().setValue(cm);
        updateCondition();

        searchService.messageProperty().addListener(new StatusListener(mc, searchService));
    }

    private void updateCondition() {
        String value = knownValue.getValueAsString();
        SearchType type = searchConditionTypeDropdown.getValue();
        ConditionType condition = searchConditionDropdown.getValue();

        String currentName = "CURRENT_VALUE";
        String prevName = "PREV_VALUE";
        String left = currentName;
        String cmp = condition.getOperator();
        String right;
        switch (type) {
            case UNKNOWN -> right = "*";
            case PREVIOUS -> right = prevName;
            case KNOWN -> right = value;
            case DIFFERENT -> {
                left = "|" + currentName + " - " + prevName + "|";
                right = value;
            }
            default -> {
                conditionLabel.setText("Invalid/Unknown condition!");
                return;
            }
        }
        conditionLabel.setFormattedText(left, cmp, right);
    }

    public void setSearchRange(long start, long end) {
        setStart(start);
        setEnd(end);
    }

    public void setStart(long start) {
        searchStart.getValueFactory().setValue(start);
        searchType.setValue(RangeType.RANGE);
    }

    public void setEnd(long end) {
        searchEnd.getValueFactory().setValue(end);
        searchType.setValue(RangeType.RANGE);
    }

    public void convertToHex(ActionEvent event) {
        try {
            long l = Long.parseUnsignedLong(knownValue.getValueAsString());
            knownValue.getValueFactory().setValue(l);
        } catch (NumberFormatException ignored) {
        }
    }

    public void poke(ActionEvent event) {
        if (isServiceRunning()) {
            MainController.showMessage(bundle.getString("search.warn.service_running"), Alert.AlertType.WARNING);
            return;
        }
        List<SearchValueModel> models = searchResults.getSelectionModel().getSelectedItems();
        long value = pokeValue.getValue();
        DataType t = dataTypeDropdown.getSelectionModel().getSelectedItem();
        Debugger debugger = mc.getDebugger();
        for (int i = 0; i < models.size(); i++) {
            SearchValueModel m = models.get(i);
            debugger.poke(t, m.getAddr(), value);
        }
    }

    public void onPageLeft(ActionEvent event) {
        currentPage--;
        updatePageInfo();
    }

    public void onPageRight(ActionEvent event) {
        currentPage++;
        updatePageInfo();
    }

    public void setMainController(MainController mc) {
        this.mc = mc;
    }

    @Override
    public void onConnect() {
        searchTabPage.setDisable(false);
    }

    @Override
    public void onDisconnect() {
        searchTabPage.setDisable(true);
    }

    public void onStartAction(ActionEvent event) {
        if (isServiceRunning()) {
            MainController.showMessage(bundle.getString("search.warn.service_running"), Alert.AlertType.WARNING);
            return;
        }
        DataType dataType = dataTypeDropdown.getValue();
        long known = knownValue.getValue();

        SearchType type = searchConditionTypeDropdown.getValue();
        ConditionType compareType = searchConditionDropdown.getValue();
        searchService.setConnection(mc.getDebugger());
        searchService.setSupplier(getDumpRegionSupplier(mc.getDebugger()));

        initSearch(type, compareType, dataType, known);
    }

    public void onRestartAction(ActionEvent event) {
        if (isServiceRunning()) {
            MainController.showMessage(bundle.getString("search.warn.service_running_cancel"),
                    Alert.AlertType.WARNING);
            return;
        }
        searchService.clear();
        DoubleProperty prog = mc.getProgressBar().progressProperty();
        prog.unbind();
        prog.setValue(0);
        if (result != null) {
            try {
                result.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        result = null;
        currentPage = 0;
        updatePageInfo();
        mc.setStatus("search.status.cleared");
    }

    public void onCancelAction(ActionEvent event) {
        if (runningService == null) {
            MainController.showMessage(bundle.getString("search.info.not_run"), Alert.AlertType.INFORMATION);
            return;
        }
        if (!runningService.isRunning()) {
            MainController.showMessage(bundle.getString("search.warn.not_running"), Alert.AlertType.WARNING);
        }
        if (runningService.cancel()) {
            mc.getProgressBar().progressProperty().unbind();
            mc.setStatus("search.status.canceled");
            searchOptions.setDisable(false);
        }
    }

    public void onUndoAction(ActionEvent event) {
        if (isServiceRunning()) {
            MainController.showMessage(bundle.getString("search.warn.undo_wait"),
                    Alert.AlertType.WARNING);
            return;
        }
        if (result == null) {
            MainController.showMessage(bundle.getString("search.warn.undo_no_result"),
                    Alert.AlertType.WARNING);
            return;
        }
        if (result.getPrev() == null) {
            MainController.showMessage(bundle.getString("search.warn.undo_no_prev"), Alert.AlertType.WARNING);
            return;
        }
        SearchResult prev = result.getPrev();
        result.setPrev(null); //to prevent the result from closing the previous one
        try {
            result.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        result = prev;
        searchService.setPrevResult(result);
        updatePageInfo();
    }

    private void initSearch(SearchType type, ConditionType compareType,
                            DataType dataType, long known) {
        searchService.setType(type);
        searchService.setCompareType(compareType);
        searchService.setDataType(dataType);
        searchService.setKnownValue(known);

        searchService.setOnSucceeded(v -> {
            result = (SearchResult) v.getSource().getValue();
            searchService.setPrevResult(result);
            mc.setStatus("search.status.complete", result.size());
            resultList.clear();
            currentPage = 0;
            searchOptions.setDisable(false);
            if (result.getType() == SearchType.UNKNOWN) {
                searchConditionTypeDropdown.setValue(SearchType.PREVIOUS);
                searchConditionDropdown.setValue(ConditionType.NOT_EQUAL);
            }
            updatePageInfo();
        });

        searchService.setOnFailed(value -> {
            searchService.setPrevResult(null);
            mc.setStatus("search.status.failed");
            Throwable t = value.getSource().getException();
            t.printStackTrace();
            MainController.showMessage(t);
            searchOptions.setDisable(false);
        });
        searchOptions.setDisable(true);
        initService(searchService);
    }

    private void updatePageInfo() {
        int maxPages = result == null ? 0 : result.getPageCount();
        int size = result == null ? 0 : result.size();
        pageLeft.setDisable(currentPage == 0);
        pageRight.setDisable(currentPage >= maxPages - 1);
        if (maxPages == 0) {
            pageLabel.setFormattedText(0, 0, size);
        } else {
            pageLabel.setFormattedText(currentPage + 1, maxPages, size);
        }
        resultList.clear();

        if (result != null) {
            List<Long> addrs = result.getPage(currentPage);
            for (int i = 0; i < addrs.size(); i++) {
                long addr = addrs.get(i);
                try {
                    long curr = result.getCurr(addr);
                    resultList.add(new SearchValueModel(addr, result.getPrev(addr), curr));
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    private void initService(Service<?> service) {
        DoubleProperty p = mc.getProgressBar().progressProperty();
        p.unbind();
        p.bind(service.progressProperty());
        service.messageProperty().addListener((observable, oldValue, newValue) -> mc.setStatus(newValue));
        this.runningService = service;
        runningService.restart();
    }

    private boolean isServiceRunning() {
        return runningService != null && runningService.isRunning();
    }

    private DumpRegionSupplier getDumpRegionSupplier(Debugger conn) {
        if (result != null && result.size() > 0) {
            return DumpRegionSupplier.createSupplierFromRange(conn, result.getStart(), result.getEnd());
        }
        RangeType t = searchType.getValue();
        switch (t) {
            case RANGE:
                long start = searchStart.getValue();
                long end = searchEnd.getValue();
                return DumpRegionSupplier.createSupplierFromRange(conn, start, end);
            case ALL:
                return DumpRegionSupplier.createSupplierFromInfo(conn, info -> info.isReadable() && info.isWriteable());
            case HEAP:
                return DumpRegionSupplier.createSupplierFromInfo(conn, info -> info.isReadable() && info.getType() == MemoryType.HEAP);
            case TLS:
                return DumpRegionSupplier.createSupplierFromInfo(conn, info -> info.isReadable() && info.getType() == MemoryType.THREAD_LOCAL);
        }
        return null;
    }

}
