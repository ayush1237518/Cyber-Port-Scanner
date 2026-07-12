package com.portscanner.ui;

import com.portscanner.exporter.ExporterFactory;
import com.portscanner.exporter.ExporterFactory.ExportFormat;
import com.portscanner.exporter.ReportExporter;
import com.portscanner.model.PortStatus;
import com.portscanner.model.ScanConfig;
import com.portscanner.model.ScanResult;
import com.portscanner.model.ScanStatistics;
import com.portscanner.scanner.PortScanner;
import com.portscanner.scanner.ScanProgressListener;
import com.portscanner.utils.AppLogger;
import com.portscanner.utils.TimerUtil;
import com.portscanner.validation.InputValidator;
import com.portscanner.validation.ValidationException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controller for the main application window.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Reads and validates user input</li>
 *     <li>Runs scans on a background {@link Task} to keep the UI responsive</li>
 *     <li>Marshals progress callbacks back onto the JavaFX Application Thread</li>
 *     <li>Wires up filtering, sorting, export, and keyboard shortcuts</li>
 * </ul>
 */
public class ScannerController {

    // ---- Configuration inputs ----
    @FXML private TextField targetField;
    @FXML private TextField startPortField;
    @FXML private TextField endPortField;
    @FXML private TextField timeoutField;
    @FXML private TextField threadCountField;
    @FXML private CheckBox bannerGrabCheckBox;

    // ---- Controls ----
    @FXML private Button scanButton;
    @FXML private Button stopButton;
    @FXML private TextField filterField;

    // ---- Progress ----
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Label elapsedTimeLabel;
    @FXML private Label remainingTimeLabel;
    @FXML private Label statusPill;

    // ---- Summary ----
    @FXML private Label totalPortsLabel;
    @FXML private Label openPortsLabel;
    @FXML private Label closedPortsLabel;
    @FXML private Label filteredPortsLabel;
    @FXML private Label durationLabel;

    // ---- Table ----
    @FXML private TableView<ScanResult> resultsTable;
    @FXML private TableColumn<ScanResult, Integer> portColumn;
    @FXML private TableColumn<ScanResult, String> statusColumn;
    @FXML private TableColumn<ScanResult, String> serviceColumn;
    @FXML private TableColumn<ScanResult, Long> responseTimeColumn;
    @FXML private TableColumn<ScanResult, String> bannerColumn;

    // ---- Log ----
    @FXML private TextArea logArea;
    @FXML private javafx.scene.layout.BorderPane rootPane;

    private final ObservableList<ScanResult> masterResults = FXCollections.observableArrayList();
    private final List<ScanResult> threadSafeBuffer = new CopyOnWriteArrayList<>();

    private PortScanner activeScanner;
    private Thread scanThread;
    private Timeline elapsedTimer;
    private long scanStartMillis;
    private volatile int lastCompleted = 0;
    private volatile int lastTotal = 0;

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Called automatically by the FXMLLoader after all @FXML fields are injected.
     */
    @FXML
    public void initialize() {
        configureTableColumns();
        configureFiltering();
        configureLogging();
        configureKeyboardShortcuts();
        AppLogger.info("Application initialized. Ready to scan.");
    }

    // ==================================================================
    // Table setup
    // ==================================================================

    private void configureTableColumns() {
        portColumn.setCellValueFactory(new PropertyValueFactory<>("port"));
        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus().getDisplayName()));
        serviceColumn.setCellValueFactory(new PropertyValueFactory<>("serviceName"));
        responseTimeColumn.setCellValueFactory(new PropertyValueFactory<>("responseTimeMillis"));
        bannerColumn.setCellValueFactory(cellData -> {
            String banner = cellData.getValue().getBanner();
            return new SimpleStringProperty(banner.isEmpty() ? "—" : banner);
        });

        // Color-code the status column based on port state.
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("cell-open", "cell-closed", "cell-filtered", "cell-error");
                if (empty || status == null) {
                    setText(null);
                    return;
                }
                setText(status);
                if (status.equals(PortStatus.OPEN.getDisplayName())) {
                    getStyleClass().add("cell-open");
                } else if (status.equals(PortStatus.CLOSED.getDisplayName())) {
                    getStyleClass().add("cell-closed");
                } else if (status.equals(PortStatus.FILTERED.getDisplayName())) {
                    getStyleClass().add("cell-filtered");
                } else {
                    getStyleClass().add("cell-error");
                }
            }
        });

        responseTimeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Long value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value + " ms");
            }
        });

        FilteredList<ScanResult> filtered = new FilteredList<>(masterResults, r -> true);
        SortedList<ScanResult> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(resultsTable.comparatorProperty());
        resultsTable.setItems(sorted);
        this.filteredResults = filtered;
    }

    // Kept as a field so the filter predicate can be updated from the search box.
    private FilteredList<ScanResult> filteredResults;

    // ==================================================================
    // Filtering
    // ==================================================================

    private void configureFiltering() {
        filterField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));
    }

    private void applyFilter(String query) {
        if (filteredResults == null) {
            return;
        }
        String normalized = query == null ? "" : query.trim().toLowerCase();
        filteredResults.setPredicate(result -> {
            if (normalized.isEmpty()) {
                return true;
            }
            return String.valueOf(result.getPort()).contains(normalized)
                    || result.getStatus().getDisplayName().toLowerCase().contains(normalized)
                    || result.getServiceName().toLowerCase().contains(normalized)
                    || result.getBanner().toLowerCase().contains(normalized);
        });
    }

    // ==================================================================
    // Logging panel
    // ==================================================================

    private void configureLogging() {
        AppLogger.setUiSink(line -> Platform.runLater(() -> {
            logArea.appendText(line + System.lineSeparator());
        }));
    }

    // ==================================================================
    // Keyboard shortcuts
    // ==================================================================

    private void configureKeyboardShortcuts() {
        // Accelerators are attached once the scene is available.
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
            newScene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
                    () -> { if (!scanButton.isDisabled()) onScanClicked(); });

            newScene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.ESCAPE),
                    () -> { if (!stopButton.isDisabled()) onStopClicked(); });

            newScene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN),
                    this::onClearClicked);

            newScene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN),
                    filterField::requestFocus);
        });
    }

    // ==================================================================
    // Scan lifecycle
    // ==================================================================

    @FXML
    private void onScanClicked() {
        ScanConfig config;
        try {
            config = InputValidator.buildValidatedConfig(
                    targetField.getText(),
                    startPortField.getText(),
                    endPortField.getText(),
                    timeoutField.getText(),
                    threadCountField.getText(),
                    bannerGrabCheckBox.isSelected()
            );
        } catch (ValidationException ex) {
            showAlert(Alert.AlertType.WARNING, "Invalid Input", ex.getMessage());
            return;
        }

        onClearClicked();
        beginScanUi(config);

        Task<ScanStatistics> scanTask = buildScanTask(config);
        scanThread = new Thread(scanTask, "scan-orchestrator");
        scanThread.setDaemon(true);
        scanThread.start();
    }

    private Task<ScanStatistics> buildScanTask(ScanConfig config) {
        return new Task<>() {
            @Override
            protected ScanStatistics call() {
                ScanProgressListener listener = new ScanProgressListener() {
                    @Override
                    public void onPortScanned(ScanResult result, int completedCount, int totalCount) {
                        threadSafeBuffer.add(result);
                        lastCompleted = completedCount;
                        lastTotal = totalCount;
                        Platform.runLater(() -> {
                            masterResults.add(result);
                            updateProgressDisplay(completedCount, totalCount);
                        });
                    }

                    @Override
                    public void onScanFinished(boolean wasCancelled) {
                        Platform.runLater(() -> finishScanUi(wasCancelled));
                    }

                    @Override
                    public void onScanFailed(String reason) {
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Scan Failed", reason);
                            finishScanUi(true);
                        });
                    }
                };

                activeScanner = new PortScanner(config, listener);
                return activeScanner.runScan();
            }
        };
    }

    private void beginScanUi(ScanConfig config) {
        scanStartMillis = System.currentTimeMillis();
        lastCompleted = 0;
        lastTotal = config.getTotalPorts();

        scanButton.setDisable(true);
        stopButton.setDisable(false);
        progressBar.setProgress(0);
        progressLabel.setText("0%% — 0 / %d ports".formatted(lastTotal));
        statusPill.setText("SCANNING");
        statusPill.getStyleClass().removeAll("status-idle", "status-complete", "status-stopped");
        statusPill.getStyleClass().add("status-scanning");

        elapsedTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateElapsedAndRemaining()));
        elapsedTimer.setCycleCount(Timeline.INDEFINITE);
        elapsedTimer.play();

        AppLogger.info("Scan requested: " + config);
    }

    private void updateProgressDisplay(int completed, int total) {
        double progress = total == 0 ? 0 : (double) completed / total;
        progressBar.setProgress(progress);
        progressLabel.setText("%.1f%% — %d / %d ports".formatted(
                TimerUtil.percentComplete(completed, total), completed, total));
    }

    private void updateElapsedAndRemaining() {
        long elapsed = System.currentTimeMillis() - scanStartMillis;
        elapsedTimeLabel.setText(TimerUtil.formatDuration(elapsed));

        long remaining = TimerUtil.estimateRemainingMillis(elapsed, lastCompleted, lastTotal);
        remainingTimeLabel.setText(remaining > 0 ? TimerUtil.formatDuration(remaining) : "--:--:--");
    }

    private void finishScanUi(boolean wasCancelled) {
        if (elapsedTimer != null) {
            elapsedTimer.stop();
        }
        scanButton.setDisable(false);
        stopButton.setDisable(true);

        statusPill.getStyleClass().removeAll("status-idle", "status-scanning", "status-complete", "status-stopped");
        statusPill.setText(wasCancelled ? "STOPPED" : "COMPLETE");
        statusPill.getStyleClass().add(wasCancelled ? "status-stopped" : "status-complete");

        long duration = System.currentTimeMillis() - scanStartMillis;
        List<ScanResult> snapshot = List.copyOf(threadSafeBuffer);
        ScanStatistics stats = new ScanStatistics(snapshot, duration);
        updateSummaryCard(stats);
        remainingTimeLabel.setText("00:00:00");

        AppLogger.info("UI updated with final scan statistics.");
    }

    private void updateSummaryCard(ScanStatistics stats) {
        totalPortsLabel.setText(String.valueOf(stats.getTotalPorts()));
        openPortsLabel.setText(String.valueOf(stats.getOpenCount()));
        closedPortsLabel.setText(String.valueOf(stats.getClosedCount()));
        filteredPortsLabel.setText(String.valueOf(stats.getFilteredCount()));
        durationLabel.setText(TimerUtil.formatDuration(stats.getScanDurationMillis()));
    }

    @FXML
    private void onStopClicked() {
        if (activeScanner != null) {
            activeScanner.cancel();
        }
        stopButton.setDisable(true);
    }

    @FXML
    private void onClearClicked() {
        masterResults.clear();
        threadSafeBuffer.clear();
        progressBar.setProgress(0);
        progressLabel.setText("0% — 0 / 0 ports");
        elapsedTimeLabel.setText("00:00:00");
        remainingTimeLabel.setText("--:--:--");
        totalPortsLabel.setText("0");
        openPortsLabel.setText("0");
        closedPortsLabel.setText("0");
        filteredPortsLabel.setText("0");
        durationLabel.setText("00:00:00");
        statusPill.getStyleClass().removeAll("status-scanning", "status-complete", "status-stopped");
        statusPill.setText("IDLE");
        statusPill.getStyleClass().add("status-idle");
        AppLogger.info("Results cleared.");
    }

    // ==================================================================
    // Export
    // ==================================================================

    @FXML
    private void onExportCsv() {
        exportResults(ExportFormat.CSV);
    }

    @FXML
    private void onExportJson() {
        exportResults(ExportFormat.JSON);
    }

    @FXML
    private void onExportTxt() {
        exportResults(ExportFormat.TXT);
    }

    private void exportResults(ExportFormat format) {
        if (threadSafeBuffer.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Nothing to Export", "Run a scan before exporting a report.");
            return;
        }

        ReportExporter exporter = ExporterFactory.create(format);
        String defaultName = "portscan_%s_%s.%s".formatted(
                sanitizeFileName(targetField.getText()),
                LocalDateTime.now().format(FILE_TIMESTAMP),
                exporter.getFileExtension());

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Scan Report");
        chooser.setInitialFileName(defaultName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                exporter.getFileExtension().toUpperCase() + " Report",
                "*." + exporter.getFileExtension()));

        File file = chooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            ScanConfig config = InputValidator.buildValidatedConfig(
                    targetField.getText(), startPortField.getText(), endPortField.getText(),
                    timeoutField.getText(), threadCountField.getText(), bannerGrabCheckBox.isSelected());

            List<ScanResult> snapshot = List.copyOf(threadSafeBuffer);
            long duration = Long.parseLong(elapsedTimeToMillis(durationLabel.getText()));
            ScanStatistics stats = new ScanStatistics(snapshot, duration);

            exporter.export(Path.of(file.getAbsolutePath()), config, snapshot, stats);
            AppLogger.info("Report exported to " + file.getAbsolutePath());
            showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                    "Report saved to:\n" + file.getAbsolutePath());

        } catch (Exception ex) {
            AppLogger.error("Failed to export report.", ex);
            showAlert(Alert.AlertType.ERROR, "Export Failed", "Could not save report: " + ex.getMessage());
        }
    }

    private String elapsedTimeToMillis(String hhmmss) {
        String[] parts = hhmmss.split(":");
        long hours = Long.parseLong(parts[0]);
        long minutes = Long.parseLong(parts[1]);
        long seconds = Long.parseLong(parts[2]);
        return String.valueOf(((hours * 3600) + (minutes * 60) + seconds) * 1000);
    }

    private String sanitizeFileName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "target";
        }
        return raw.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Called by {@link MainApp} when the window is closing, to ensure any
     * running scan's thread pool is shut down cleanly and doesn't block JVM exit.
     */
    public void shutdown() {
        if (activeScanner != null) {
            activeScanner.cancel();
        }
        if (elapsedTimer != null) {
            elapsedTimer.stop();
        }
        AppLogger.info("Application shutting down.");
    }
}
