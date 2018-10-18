package me.mdbell.noexs.ui.controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import me.mdbell.noexs.io.usb.UsbUtils;
import me.mdbell.util.HexUtils;

import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbException;
import javax.usb.UsbHub;
import java.util.List;

public class UtilsController implements IController {

    public Label infoLabel;
    private MainController mc;

    public TreeView<UsbDevice> deviceTree;

    @FXML
    public void initialize() {
        deviceTree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<UsbDevice>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<UsbDevice>> observable, TreeItem<UsbDevice> oldValue, TreeItem<UsbDevice> newValue) {
                if (newValue == null) {
                    return;
                }
                UsbDevice d = newValue.getValue();
                updateInfo(d);
            }
        });
    }

    private void updateInfo(UsbDevice d) {
        if (d == null) {
            infoLabel.setText("");
            return;
        }
        try {
            StringBuilder sb = new StringBuilder("[");
            if (d.isUsbHub()) {
                sb.append("HUB");
            } else {
                sb.append("DEVICE");
            }
            sb.append("] ");
            UsbDeviceDescriptor desc = d.getUsbDeviceDescriptor();
            if (d.isUsbHub()) {
                UsbHub hub = (UsbHub) d;
                sb.append("Attached Devices: ").append(hub.getAttachedUsbDevices().size()).append(" Total Ports: ").append(hub.getNumberOfPorts());
            }else{
                sb.append("Device Id:").append(HexUtils.pad('0', 4, Integer.toUnsignedString(desc.idVendor() & 0xFFFF, 16)))
                        .append(":").append(HexUtils.pad('0', 4, Integer.toUnsignedString(desc.idProduct() & 0xFFFF, 16)));
                sb.append(" Is Switch:").append(UsbUtils.isSwitch(d));
            }
            infoLabel.setText(sb.toString());
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setMainController(MainController c) {
        this.mc = c;
    }

    @Override
    public void onConnect() {

    }

    @Override
    public void onDisconnect() {

    }

    public void onRefresh(ActionEvent event) throws UsbException {
        updateInfo(null);
        UsbHub hub = UsbUtils.getRootHub();
        TreeItem<UsbDevice> root = new TreeItem<>(hub);
        scanHub(root, hub);
        deviceTree.setRoot(root);
    }

    private void scanHub(TreeItem<UsbDevice> root, UsbHub hub) {
        root.setExpanded(true);
        List<UsbDevice> deviceList = hub.getAttachedUsbDevices();
        for (UsbDevice d : deviceList) {
            TreeItem<UsbDevice> node = new TreeItem<>(d);
            if (d.isUsbHub()) {
                scanHub(node, (UsbHub) d);
            }
            root.getChildren().add(node);
        }

    }
}
