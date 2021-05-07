package me.mdbell.noexs.ui.controllers;

import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public interface IController extends Initializable {

    void setMainController(MainController c);

    void onConnect();

    void onDisconnect();

    @Override
    default void initialize(URL url, ResourceBundle bundle) {

    }

    default void stop(){

    }
}
