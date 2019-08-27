module noexs {
    requires java.sql;
    requires java.desktop;
    requires java.prefs;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires org.objectweb.asm;
    requires usb.api;

    opens me.mdbell.noexs.ui.controllers to javafx.fxml;

    exports me.mdbell.javafx.control to javafx.fxml;
    exports me.mdbell.noexs.ui to javafx.graphics;
}
