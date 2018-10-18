package me.mdbell.noexs.ui.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import me.mdbell.javafx.control.AddressSpinner;
import me.mdbell.noexs.core.Result;
import me.mdbell.util.CsToolWrapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class DisassemblerController implements IController {

    @FXML
    AddressSpinner visibleAddress;

    @FXML
    ListView<CsToolWrapper.Insn> instructionList;

    @FXML
    AddressSpinner selectedAddress;

    @FXML
    TextField selectedOp;

    @FXML
    TitledPane visiblePane;

    @FXML
    TitledPane selectedPane;

    @FXML
    TitledPane callstackPane;

    private MainController mc;

    @FXML
    public void initialize() {
        instructionList.getSelectionModel().selectedItemProperty().addListener(event -> {
            CsToolWrapper.Insn i = instructionList.getSelectionModel().getSelectedItem();
            if (i == null) {
                return;
            }
            selectedAddress.getValueFactory().setValue(i.getAddr());
            selectedOp.setText(i.getOpStr());
        });
    }

    void updateView() {
        //TODO do this as a service
        byte[] buffer = new byte[2048];
        long addr = visibleAddress.getValue();

        ByteBuffer byteBuffer = mc.getDebugger().readmem(addr, buffer.length, buffer).order(ByteOrder.LITTLE_ENDIAN);
        instructionList.getItems().clear();
        try {
            List<CsToolWrapper.Insn> insns = CsToolWrapper.disasm(buffer, addr);
            instructionList.getItems().addAll(insns);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setDisassembleAddress(long address) {
        visibleAddress.getValueFactory().setValue(address);
        updateView();
    }

    @Override
    public void setMainController(MainController c) {
        this.mc = c;
    }

    @Override
    public void onConnect() {
        visiblePane.setDisable(false);
        selectedPane.setDisable(false);
        callstackPane.setDisable(false);
    }

    @Override
    public void onDisconnect() {
        visiblePane.setDisable(true);
        selectedPane.setDisable(true);
        callstackPane.setDisable(true);
    }

    public void update(ActionEvent event) {
        updateView();
    }

    public void assembleOp(ActionEvent event) {
        long addr = selectedAddress.getValue();
        String op = selectedOp.getText();
        try {
            byte[] b = CsToolWrapper.assemble(op, addr);
            if(b == null) {
                return;
            }
            Result r = mc.getDebugger().writemem(b, addr);
            System.out.println(r);
            if(r.succeeded()) {
                updateView();
            }else{
                MainController.showMessage("Unable to assemble \"" + op + "\"", Alert.AlertType.ERROR);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
