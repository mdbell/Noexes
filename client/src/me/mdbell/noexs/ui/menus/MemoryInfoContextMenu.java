package me.mdbell.noexs.ui.menus;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import me.mdbell.noexs.ui.controllers.MainController;
import me.mdbell.noexs.ui.models.MemoryInfoTableModel;

import java.util.ResourceBundle;
import java.util.function.Supplier;

public class MemoryInfoContextMenu extends ContextMenu {

    public MemoryInfoContextMenu(ResourceBundle bundle, Supplier<MainController> mc, TableView<MemoryInfoTableModel> memInfoTable) {
        MenuItem searchBoth = new MenuItem(bundle.getString("tools.mem.ctx.search.both"));
        MenuItem searchStart = new MenuItem(bundle.getString("tools.mem.ctx.search.start"));
        MenuItem searchEnd = new MenuItem(bundle.getString("tools.mem.ctx.search.end"));
        MenuItem ptrMain = new MenuItem(bundle.getString("tools.mem.ctx.ptr.main"));
        MenuItem ptrFilter = new MenuItem(bundle.getString("tools.mem.ctx.ptr.filter.both"));
        MenuItem ptrFilterStart = new MenuItem(bundle.getString("tools.mem.ctx.ptr.filter.start"));
        MenuItem ptrFilterEnd = new MenuItem(bundle.getString("tools.mem.ctx.ptr.filter.end"));
        MenuItem memoryView = new MenuItem(bundle.getString("tools.mem.ctx.viewer"));
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
        getItems().addAll(searchBoth, searchStart, searchEnd, ptrMain, ptrFilter, ptrFilterStart, ptrFilterEnd, memoryView);
    }
}
