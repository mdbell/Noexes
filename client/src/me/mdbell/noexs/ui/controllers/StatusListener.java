package me.mdbell.noexs.ui.controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import me.mdbell.noexs.ui.services.IMessageArguments;

class StatusListener implements ChangeListener<String> {

    private final Service<?> service;
    private final MainController mc;

    public StatusListener(MainController mc, Service<?> service) {
        this.mc = mc;
        this.service = service;
    }

    @Override
    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        if (service instanceof IMessageArguments) {
            mc.setStatus(newValue, ((IMessageArguments) service).getMessageArguments());
        } else {
            mc.setStatus(newValue);
        }
    }
}
