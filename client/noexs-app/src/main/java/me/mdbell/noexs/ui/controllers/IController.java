package me.mdbell.noexs.ui.controllers;

public interface IController {

    void setMainController(MainController c);

    void onConnect();

    void onDisconnect();

    default void stop(){

    }
}
