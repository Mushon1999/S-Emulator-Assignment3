package se.emulator.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import se.emulator.server.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Execution screen for running programs and functions.
 */
public class ExecutionScreen {
    private final SEMulatorClientApp app;
    private final VBox content;
    private String currentUser;
    private String selectedProgram;
    private boolean isMainProgram;
    
    // UI Components
    private Label programNameLabel;
    private ComboBox<String> architectureComboBox;
    private Spinner<Integer> executionLevelSpinner;
    private TextField x1Field, x2Field, x3Field, x4Field, x5Field;
    private Button runButton;
    private Button debugButton;
    private TextArea outputArea;
    private TableView<ExecutionHistory> historyTable;
    private Label creditsLabel;
    private Label architectureInfoLabel;
    
    public ExecutionScreen(SEMulatorClientApp app) {
        this.app = app;
        this.content = createContent();
    }
    
    private VBox createContent() {
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        
        // Program info section
        VBox programInfoBox = createProgramInfoSection();
        
        // Execution settings section
        VBox settingsBox = createExecutionSettingsSection();
        
        // Input section
        VBox inputBox = createInputSection();
        
        // Control buttons
        HBox buttonBox = createControlButtons();
        
        // Output section
        VBox outputBox = createOutputSection();
        
        // History section
        VBox historyBox = createHistorySection();
        
        mainContainer.getChildren().addAll(
            programInfoBox,
            settingsBox,
            inputBox,
            buttonBox,
            outputBox,
            historyBox
        );
        
        return mainContainer;
    }
    
    private VBox createProgramInfoSection() {
        VBox container = new VBox(10);
        
        programNameLabel = new Label("No program selected");
        programNameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        creditsLabel = new Label("Credits: Loading...");
        
        container.getChildren().addAll(programNameLabel, creditsLabel);
        return container;
    }
    
    private VBox createExecutionSettingsSection() {
        VBox container = new VBox(10);
        
        // Architecture selection
        HBox architectureBox = new HBox(10);
        architectureBox.getChildren().add(new Label("Architecture:"));
        architectureComboBox = new ComboBox<>();
        architectureComboBox.getItems().addAll("I", "II", "III", "IV");
        architectureComboBox.setValue("I");
        architectureComboBox.setOnAction(e -> updateArchitectureInfo());
        architectureBox.getChildren().add(architectureComboBox);
        
        // Execution level
        HBox levelBox = new HBox(10);
        levelBox.getChildren().add(new Label("Execution Level:"));
        executionLevelSpinner = new Spinner<>(0, 10, 0);
        executionLevelSpinner.setEditable(true);
        levelBox.getChildren().add(executionLevelSpinner);
        
        // Architecture info
        architectureInfoLabel = new Label();
        architectureInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        updateArchitectureInfo();
        
        container.getChildren().addAll(
            new Label("Execution Settings:"),
            architectureBox,
            levelBox,
            architectureInfoLabel
        );
        
        return container;
    }
    
    private VBox createInputSection() {
        VBox container = new VBox(10);
        
        HBox inputBox = new HBox(20);
        inputBox.getChildren().addAll(
            new Label("x1:"), x1Field = new TextField("0"),
            new Label("x2:"), x2Field = new TextField("0"),
            new Label("x3:"), x3Field = new TextField("0"),
            new Label("x4:"), x4Field = new TextField("0"),
            new Label("x5:"), x5Field = new TextField("0")
        );
        
        // Set field widths
        for (TextField field : new TextField[]{x1Field, x2Field, x3Field, x4Field, x5Field}) {
            field.setPrefWidth(80);
        }
        
        container.getChildren().addAll(
            new Label("Input Values:"),
            inputBox
        );
        
        return container;
    }
    
    private HBox createControlButtons() {
        HBox buttonBox = new HBox(10);
        
        runButton = new Button("Run Program");
        runButton.setPrefWidth(120);
        runButton.setOnAction(e -> handleRun());
        
        debugButton = new Button("Debug Program");
        debugButton.setPrefWidth(120);
        debugButton.setOnAction(e -> handleDebug());
        
        Button backButton = new Button("Back to Programs");
        backButton.setOnAction(e -> app.getMainTabPane().getSelectionModel().select(1));
        
        buttonBox.getChildren().addAll(runButton, debugButton, backButton);
        
        return buttonBox;
    }
    
    private VBox createOutputSection() {
        VBox container = new VBox(10);
        
        outputArea = new TextArea();
        outputArea.setPrefRowCount(8);
        outputArea.setEditable(false);
        
        container.getChildren().addAll(
            new Label("Execution Output:"),
            outputArea
        );
        
        return container;
    }
    
    private VBox createHistorySection() {
        VBox container = new VBox(10);
        
        historyTable = new TableView<>();
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        historyTable.setPrefHeight(200);
        
        TableColumn<ExecutionHistory, Integer> runCol = new TableColumn<>("Run #");
        runCol.setCellValueFactory(new PropertyValueFactory<>("runNumber"));
        
        TableColumn<ExecutionHistory, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().isMainProgram() ? "Program" : "Function"
            )
        );
        
        TableColumn<ExecutionHistory, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("programOrFunctionName"));
        
        TableColumn<ExecutionHistory, String> archCol = new TableColumn<>("Architecture");
        archCol.setCellValueFactory(new PropertyValueFactory<>("architectureType"));
        
        TableColumn<ExecutionHistory, Integer> levelCol = new TableColumn<>("Level");
        levelCol.setCellValueFactory(new PropertyValueFactory<>("executionLevel"));
        
        TableColumn<ExecutionHistory, Long> resultCol = new TableColumn<>("Result (y)");
        resultCol.setCellValueFactory(new PropertyValueFactory<>("finalYValue"));
        
        TableColumn<ExecutionHistory, Integer> cyclesCol = new TableColumn<>("Cycles");
        cyclesCol.setCellValueFactory(new PropertyValueFactory<>("cyclesConsumed"));
        
        TableColumn<ExecutionHistory, Long> costCol = new TableColumn<>("Cost");
        costCol.setCellValueFactory(new PropertyValueFactory<>("creditsSpent"));
        
        historyTable.getColumns().addAll(runCol, typeCol, nameCol, archCol, levelCol, resultCol, cyclesCol, costCol);
        
        container.getChildren().addAll(
            new Label("Execution History:"),
            historyTable
        );
        
        return container;
    }
    
    private void updateArchitectureInfo() {
        String selectedArch = architectureComboBox.getValue();
        if (selectedArch != null) {
            Map<String, Object> architectures = app.getServerService().getArchitectures();
            Object archInfo = architectures.get(selectedArch);
            if (archInfo != null) {
                architectureInfoLabel.setText(
                    String.format("Architecture: %s", selectedArch)
                );
            }
        }
    }
    
    private void handleRun() {
        if (selectedProgram == null) {
            app.showErrorAlert("No Program Selected", "Please select a program or function first.");
            return;
        }
        
        try {
            Map<String, Long> inputs = getInputValues();
            String architecture = architectureComboBox.getValue();
            int level = executionLevelSpinner.getValue();
            
            ExecutionResult result = app.getServerService().executeProgram(
                currentUser, selectedProgram, inputs, level);
            
            if (result.isSuccess()) {
                outputArea.appendText(String.format(
                    "Execution completed successfully!\n" +
                    "Result (y): %d\n" +
                    "Cycles: %d\n" +
                    "Total Cost: %d credits\n\n",
                    result.getYValue(), result.getCycles(), result.getCost()
                ));
                updateUserCredits();
                updateHistory();
            } else {
                app.showErrorAlert("Execution Failed", result.getMessage());
            }
        } catch (Exception e) {
            app.showErrorAlert("Execution Error", "Error during execution: " + e.getMessage());
        }
    }
    
    private void handleDebug() {
        // For now, just show a message that debug mode is not implemented
        app.showAlert("Debug Mode", "Debug mode will be implemented in the next version.");
    }
    
    private Map<String, Long> getInputValues() {
        Map<String, Long> inputs = new HashMap<>();
        inputs.put("x1", parseLong(x1Field.getText()));
        inputs.put("x2", parseLong(x2Field.getText()));
        inputs.put("x3", parseLong(x3Field.getText()));
        inputs.put("x4", parseLong(x4Field.getText()));
        inputs.put("x5", parseLong(x5Field.getText()));
        return inputs;
    }
    
    private Long parseLong(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    public void setCurrentUser(String username) {
        this.currentUser = username;
        updateUserCredits();
        updateHistory();
    }
    
    public void setSelectedProgram(String programName, boolean isMainProgram) {
        this.selectedProgram = programName;
        this.isMainProgram = isMainProgram;
        
        programNameLabel.setText(String.format("Selected: %s (%s)", 
            programName, isMainProgram ? "Program" : "Function"));
        
        runButton.setDisable(false);
        debugButton.setDisable(false);
    }
    
    public void updateUserCredits() {
        if (currentUser != null) {
            List<User> users = app.getServerService().getAllUsers();
            User currentUserObj = users.stream()
                .filter(u -> u.getName().equals(currentUser))
                .findFirst()
                .orElse(null);
            if (currentUserObj != null) {
                creditsLabel.setText("Credits: " + currentUserObj.getCredits());
            }
        }
    }
    
    public void updateHistory() {
        if (currentUser != null) {
            List<ExecutionHistory> history = app.getServerService().getUserExecutionHistory(currentUser);
            historyTable.setItems(FXCollections.observableArrayList(history));
        }
    }
    
    public void clearData() {
        selectedProgram = null;
        programNameLabel.setText("No program selected");
        creditsLabel.setText("Credits: Not logged in");
        outputArea.clear();
        historyTable.setItems(FXCollections.emptyObservableList());
        runButton.setDisable(true);
        debugButton.setDisable(true);
    }
    
    public VBox getContent() {
        return content;
    }
}
