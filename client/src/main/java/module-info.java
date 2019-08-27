module noexs {
    requires java.desktop;
    requires java.prefs;
    requires org.objectweb.asm;
    requires usb.api;
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.base;
    requires transitive javafx.graphics;

    opens me.mdbell.noexs.ui.controllers to javafx.fxml;

    exports me.mdbell.javafx.control to javafx.fxml;
    exports me.mdbell.noexs.ui to javafx.graphics;
}
