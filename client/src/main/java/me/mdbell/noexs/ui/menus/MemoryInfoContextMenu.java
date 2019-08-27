package me.mdbell.noexs.ui.menus;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import me.mdbell.noexs.ui.controllers.MainController;
import me.mdbell.noexs.ui.models.MemoryInfoTableModel;

import java.util.function.Supplier;

public class MemoryInfoContextMenu extends ContextMenu {

    public MemoryInfoContextMenu(Supplier<MainController> mc, TableView<MemoryInfoTableModel> memInfoTable) {
        MenuItem searchBoth = new MenuItem("Search (Start & End)");
        MenuItem searchStart = new MenuItem("Search(Start)");
        MenuItem searchEnd = new MenuItem("Search (End)");
        MenuItem ptrMain = new MenuItem("Pointer Search (Main)");
        MenuItem ptrFilter = new MenuItem("Pointer Search (Filter Min & Min)");
        MenuItem ptrFilterStart = new MenuItem("Pointer Search (Filter Min)");
        MenuItem ptrFilterEnd = new MenuItem("Pointer Search (Filter Max)");
        MenuItem memoryView = new MenuItem("Memory Viewer");
        MenuItem disassembler = new MenuItem("Disassembler");
        searchBoth.setOnAction(event -> {
            MemoryInfoTableModel model = memInfoTable.getSelectionModel().getSelectedItem();
            if (model == null) {
                return;
            }
            mc.get().search().setSearchRange(model.getAddr(), model.getEnd());
            mc.get().setTab(MainController.Tab.SEARCH);
        });
        searchStart.setOnAction(event -> {
            MemoryInfoTableModel model = memInfoTable.getSelectionModel().getSelectedItem();
            if (model == null) {
                return;
            }
            mc.get().search().setStart(model.getAddr());
        });

        ptrMain.setOnAction(event -> {
            MemoryInfoTableModel model = memInfoTable.getSelectionModel().getSelectedItem();
            if (model == null) {
                return;
            }
            mc.get().pointer().setRelativeAddress(model.getAddr());
        });

        ptrFilter.setOnAction(event -> {
            MemoryInfoTableModel model = memInfoTable.getSelectionModel().getSelectedItem();
            if (model == null) {
                return;
            }
            mc.get().pointer().setFilterMin(model.getAddr());
            mc.get().pointer().setFilterMax(model.getEnd());
            mc.get().setTab(MainController.Tab.POINTER_SEARCH);
        });

        ptrFilterStart.setOnAction(event -> {
            MemoryInfoTableModel model = memInfoTable.getSelectionModel().getSelectedItem();
            if (model == null) {
                return;
            }
            mc.get().pointer().setFilterMin(model.getAddr());
        });

        ptrFilterEnd.setOnAction(event -> {
            MemoryInfoTableModel model = memInfoTable.getSelectionModel().getSelectedItem();
            if (model == null) {
                return;
            }
            mc.get().pointer().setFilterMax(model.getEnd());
        });

        searchEnd.setOnAction(event -> {
            MemoryInfoTableModel model = memInfoTable.getSelectionModel().getSelectedItem();
            if (model == null) {
                return;
            }
            mc.get().search().setEnd(model.getEnd());
        });
        memoryView.setOnAction(event -> {
            MemoryInfoTableModel model = memInfoTable.getSelectionModel().getSelectedItem();
            if (model == null) {
                return;
            }
            mc.get().memory().setViewAddress(model.getAddr());
            mc.get().setTab(MainController.Tab.MEMORY_VIEWER);

        });
        disassembler.setOnAction(event -> {
            MemoryInfoTableModel model = memInfoTable.getSelectionModel().getSelectedItem();
            if (model == null) {
                return;
            }
            mc.get().disassembly().setDisassembleAddress(model.getAddr());
            mc.get().setTab(MainController.Tab.DISASSEMBLER);
        });
        getItems().addAll(searchBoth, searchStart, searchEnd, ptrMain, ptrFilter, ptrFilterStart, ptrFilterEnd, memoryView, disassembler);
    }
}
