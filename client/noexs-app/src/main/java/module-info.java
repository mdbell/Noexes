module noexs.app {
    requires noexs.utils;
    requires java.sql;
    requires java.desktop;
    requires java.prefs;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires usb.api;

    opens me.mdbell.noexs.ui.controllers to javafx.fxml;

    exports me.mdbell.noexs.ui to javafx.graphics;
    exports me.mdbell.noexs.ui.controllers to javafx.fxml;
}
