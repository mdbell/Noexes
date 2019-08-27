package me.mdbell.noexs.ui.controllers;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.mdbell.noexs.core.DebuggerStatus;
import me.mdbell.noexs.core.IConnection;
import me.mdbell.noexs.io.net.NetworkConstants;
import me.mdbell.noexs.core.Debugger;
import me.mdbell.noexs.core.Result;
import me.mdbell.noexs.misc.NopConnection;
import me.mdbell.noexs.misc.ResultDecoder;
import me.mdbell.noexs.ui.NoexsApplication;
import me.mdbell.noexs.ui.Settings;
import me.mdbell.noexs.ui.models.ConnectionType;
import me.mdbell.noexs.ui.services.DebuggerConnectionService;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;


public class MainController implements NetworkConstants, IController {

    public ChoiceBox<ConnectionType> connectionType;
    public CheckBox autoResume;
    private Debugger debugger = new Debugger(NopConnection.INSTANCE);

    /* Global Elements */
    @FXML
    Button connectBtn;
    @FXML
    Label statusLbl;
    @FXML
    TextField ipAddr;
    @FXML
    ProgressBar progressBar;

    @FXML
    Label progressLabel;

    @FXML
    TabPane tabs;

    @FXML
    ToolsController toolsTabPageController;

    @FXML
    MemoryViewerController memViewTabPageController;

    @FXML
    SearchController searchTabPageController;

    @FXML
    DisassemblerController disassembleTabPageController;

    @FXML
    PointerSearchController pointerTabPageController;

    @FXML
    WatchlistController watchlistTabPageController;

    @FXML
    UtilsController utilsTabPageController;

    private final List<IController> controllers = new LinkedList<>();

    private final DebuggerConnectionService connectionService = new DebuggerConnectionService();

    private final Timer timer = new Timer(true);

    private Stage stage;

    private FileChooser fileChooser = new FileChooser();

    public void setStage(Stage s) {
        if (stage != null) {
            return;
        }
        stage = s;
    }

    @FXML
    public void initialize() {
        fileChooser.setInitialDirectory(Settings.getChooserDir());
        controllers.add(this);
        controllers.add(toolsTabPageController);
        controllers.add(memViewTabPageController);
        controllers.add(searchTabPageController);
        controllers.add(disassembleTabPageController);
        controllers.add(pointerTabPageController);
        controllers.add(watchlistTabPageController);
        controllers.add(utilsTabPageController);

        fire(c -> c.setMainController(this));
        fire(IController::onDisconnect);

        progressBar.progressProperty().addListener((observable, oldValue, newValue) -> {
            int i = (int) (newValue.doubleValue() * 100);
            if (i < 0) {
                i = 0;
            } else if (i > 100) {
                i = 100;
            }
            progressLabel.setText(i + "%");
        });
        progressBar.setProgress(0);
        connectionService.messageProperty().addListener((observable, oldValue, newValue) -> setStatus(newValue));

        ipAddr.setText(Settings.getConnectionHost());

        connectionType.getItems().addAll(ConnectionType.values());
        connectionType.getSelectionModel().select(ConnectionType.NETWORK); //TODO save/store this
        connectionType.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> ipAddr.setDisable(newValue != ConnectionType.NETWORK));

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (debugger.connected() && autoResume.isSelected()) {
                    debugger.resume();
                }
            }
        }, 0, 100);
    }

    void fire(Consumer<IController> c) {
        controllers.forEach(c);
    }

    protected Timer timer() {
        return timer;
    }

    public ToolsController tools() {
        return toolsTabPageController;
    }

    public MemoryViewerController memory() {
        return memViewTabPageController;
    }

    public DisassemblerController disassembly() {
        return disassembleTabPageController;
    }

    @Override
    public void setMainController(MainController c) {
        //ignored
    }

    @Override
    public void onConnect() {
        setConnectBtnText("Disconnect");
        connectionService.cancel();
        setStatus("Connected");

        DebuggerStatus status = debugger.getStatus();
        setTitle(status.getStatus());
    }

    @Override
    public void onDisconnect() {
        setTitle(null);
        setConnectBtnText("Connect");
        setStatus("Connection Closed");
    }

    @FXML
    public void stop() {
        timer.cancel();
        controllers.forEach(c -> {
            if (c != this) {
                c.stop();
            }
        });
        Settings.setConnectionHost(ipAddr.getText());

        try {
            debugger.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void showMessage(String infoMessage, Result rc, Alert.AlertType alertType) {
        StringBuilder sb = new StringBuilder("\n\nReason:\n\tModule:");
        ResultDecoder.Module module = ResultDecoder.lookup(rc);
        if (module == null) {
            sb.append(rc.getModule());
        } else {
            sb.append(module.getName());
        }
        sb.append("\n\tDesc:");
        if (module == null) {
            sb.append(rc.getDesc());
        } else {
            sb.append(module.getMessage(rc.getDesc()));
        }
        showMessage(infoMessage + sb.toString(), alertType);
    }

    public static void showMessage(String infoMessage, Alert.AlertType alertType) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(NoexsApplication.APP_NAME);
            String header = null;
            switch (alertType) {
                case NONE:
                    header = "";
                    break;
                case INFORMATION:
                    header = "Info";
                    break;
                case WARNING:
                    header = "Warning";
                    break;
                case CONFIRMATION:
                    header = "Confirmation";
                    break;
                case ERROR:
                    header = "Error";
                    break;
            }
            alert.setHeaderText(header);
            alert.setContentText(infoMessage);
            alert.showAndWait();
        });
    }

    public static void showMessage(Throwable ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(ex.getClass().getName());
        alert.setHeaderText(ex.getClass().getSimpleName());
        alert.setContentText(ex.getMessage());


// Create expandable Exception.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

// Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }

    public void setTitle(String title) {
        Platform.runLater(() -> {
            Stage s = (Stage) progressBar.getScene().getWindow();
            s.setTitle(NoexsApplication.APP_NAME + " v" + NoexsApplication.APP_VERSION +
                    (title != null ? (" - " + title) : ""));
        });
    }


    public void setStatus(String message) {
        if (message.length() == 0) {
            return;
        }
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> setStatus(message));
            return;
        }
        statusLbl.setText(message);
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void setConnectBtnText(String message) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> setConnectBtnText(message));
            return;
        }
        connectBtn.setText(message);
    }

    public void continueProcess() {
        Result res = debugger.resume();
        if (res.succeeded()) {
            setStatus("Resumed");
        } else {
            showMessage("Resume failed", res, Alert.AlertType.WARNING);
        }
    }

    public void pauseGame(ActionEvent event) {
        Result res = debugger.pause();
        if (res.succeeded()) {
            setStatus("Paused");
        } else {
            showMessage("Pause failed", res, Alert.AlertType.WARNING);
        }
    }

    public void runAndWait(Runnable r) {
        if (r == null) {
            return;
        }
        if (Platform.isFxApplicationThread()) {
            r.run();
            return;
        }
        Semaphore s = new Semaphore(0);
        Platform.runLater(() -> {
            r.run();
            s.release();
        });
        try {
            s.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setConn(Debugger conn) {
        this.debugger = conn;
        fire(IController::onConnect);
    }

    public void connect(ActionEvent event) {
        connectionService.setOnSucceeded(value -> {
            IConnection conn = (IConnection) value.getSource().getValue();
            setConn(new Debugger(conn));
        });
        connectionService.setOnFailed(value -> {
            if (connectionService.getCurrentFailureCount() < connectionService.getMaximumFailureCount() - 1) {
                return;
            }
            setStatus("Unable to Connect!");
            setConnectBtnText("Connect");
            connectionType.setDisable(false);
            ipAddr.setDisable(connectionType.getSelectionModel().getSelectedItem() != ConnectionType.NETWORK);
        });

        boolean disabled = false;
        if (connectionService.isRunning()) {
            setConnectBtnText("Connect");
            connectionService.cancel();
            setStatus("Connect Canceled");
        } else if (debugger != null && debugger.connected()) { // Disconnect
            try {
                debugger.close();
                fire(IController::onDisconnect);
                connectBtn.setDisable(false);
            } catch (Exception ignored) {
            }
        } else {
            disabled = true;
            setConnectBtnText("Cancel");
            connectionService.setType(connectionType.getSelectionModel().getSelectedItem());
            connectionService.setHost(ipAddr.getText());
            connectionService.setPort(DEFAULT_PORT);
            connectionService.restart();
        }
        ipAddr.setDisable(disabled);
        connectionType.setDisable(disabled);
    }

    File browseFile(boolean save, StringProperty property, String title, String desc, String... extensions) {
        fileChooser.setTitle(title);
        List<FileChooser.ExtensionFilter> filterList = fileChooser.getExtensionFilters();
        filterList.clear();
        filterList.add(new FileChooser.ExtensionFilter(desc, extensions));
        File f = save ? fileChooser.showSaveDialog(getStage()) : fileChooser.showOpenDialog(getStage());

        if (f != null) {
            File parent = f.getParentFile();
            fileChooser.setInitialDirectory(parent);
            Settings.setChooserFile(parent);
            if(property != null) {
                property.setValue(f.toPath().toString());
            }
        }
        return f;
    }

    Debugger getDebugger() {
        return debugger;
    }

    public void setTab(Tab tab) {
        tabs.getSelectionModel().select(tab.ordinal());
    }

    public Tab getCurrentTab() {
        return Tab.values()[tabs.getSelectionModel().getSelectedIndex()];
    }

    public SearchController search() {
        return searchTabPageController;
    }

    public PointerSearchController pointer() {
        return pointerTabPageController;
    }

    public WatchlistController watch() {
        return watchlistTabPageController;
    }

    public Stage getStage() {
        return stage;
    }

    public enum Tab {
        TOOLS,
        SEARCH,
        POINTER_SEARCH,
        MEMORY_VIEWER,
        WATCH_LIST,
        DISASSEMBLER
    }
}
