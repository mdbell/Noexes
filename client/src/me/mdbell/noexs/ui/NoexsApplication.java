package me.mdbell.noexs.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import me.mdbell.noexs.misc.OffsetList;
import me.mdbell.noexs.ui.controllers.MainController;

import java.io.IOException;

public class NoexsApplication extends Application {

    public static final String APP_NAME = "JNoexs";
    public static final String APP_VERSION = "1.0.0-1";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(("views/Main.fxml")));
        Parent root = loader.load();
        stage.setResizable(false);
        stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        MainController c = loader.getController();
        c.setStage(stage);
        c.setTitle(null);
        stage.setOnHidden(v ->{
            c.stop();
            Platform.exit();
        });
        stage.show();
    }
}
