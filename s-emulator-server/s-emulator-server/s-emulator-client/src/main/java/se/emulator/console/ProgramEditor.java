package se.emulator.console;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Diligent Program Editor - A graphical program builder
 * An homage to "The Busy Beaver" problem (only nerds will get this!)
 * 
 * Allows users to create S-Emulator programs using only graphical components
 */
public class ProgramEditor {
    
    private Stage editorStage;
    private ThemeManager themeManager;
    private SEmulatorApp parentApp;
    
    // UI Components
    private TableView<InstructionRow> instructionsTable;
    private ComboBox<String> commandTypeSelector;
    private TextField variableField;
    private TextField valueField;
    private Button addInstructionBtn;
    private Button editInstructionBtn;
    private Button deleteInstructionBtn;
    private Button saveBtn;
    private Button loadIntoEmulatorBtn;
    private Label statusLabel;
    
    // Program data
    private List<InstructionRow> instructions;
    private File currentFile;
    
    /**
     * Represents an instruction row in the editor
     */
    public static class InstructionRow {
        private String type;
        private String variable;
        private String value;
        private int lineNumber;
        
        public InstructionRow(int lineNumber, String type, String variable, String value) {
            this.lineNumber = lineNumber;
            this.type = type;
            this.variable = variable != null ? variable : "";
            this.value = value != null ? value : "";
        }
        
        // Getters and setters
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getVariable() { return variable; }
        public void setVariable(String variable) { this.variable = variable; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        
        public String getDescription() {
            switch (type) {
                case "ASSIGNMENT":
                    return variable + " ← " + value;
                case "INCREASE":
                case "DECREASE":
                    return type + "(" + variable + ")";
                case "JUMP_NOT_ZERO":
                    return "IF " + variable + " ≠ 0 GOTO " + value;
                case "GOTO_LABEL":
                    return "GOTO " + value;
                case "LABEL":
                    return "LABEL " + value;
                default:
                    return type;
            }
        }
    }
    
    public ProgramEditor(ThemeManager themeManager, SEmulatorApp parentApp) {
        this.themeManager = themeManager;
        this.parentApp = parentApp;
        this.instructions = new ArrayList<>();
        initializeEditor();
    }
    
    private void initializeEditor() {
        editorStage = new Stage();
        editorStage.setTitle("Diligent Program Editor 🦫 - The Busy Beaver's Programming Tool");
        editorStage.initModality(Modality.NONE);
        editorStage.setResizable(true);
        
        // Create main layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle(themeManager.getMainBackgroundStyle());
        
        // Create and set up all UI components
        createTopPanel(mainLayout);
        createCenterPanel(mainLayout);
        createBottomPanel(mainLayout);
        
        // Create scene
        Scene scene = new Scene(mainLayout, 900, 700);
        editorStage.setScene(scene);
        
        // Apply theme
        applyTheme();
        
        // Initialize with empty program
        refreshInstructionsTable();
        updateStatus("Ready to create a new program");
    }
    
    private void createTopPanel(BorderPane mainLayout) {
        VBox topPanel = new VBox(15);
        topPanel.setPadding(new Insets(20));
        topPanel.setStyle(themeManager.getAccent1BackgroundStyle());
        
        // Title
        Label titleLabel = new Label("🦫 Diligent Program Editor");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;" + themeManager.getPrimaryTextStyle());
        
        Label subtitleLabel = new Label("Build S-Emulator programs using graphical components");
        subtitleLabel.setStyle("-fx-font-size: 14px;" + themeManager.getSecondaryTextStyle());
        
        // Command input area
        GridPane inputArea = new GridPane();
        inputArea.setHgap(10);
        inputArea.setVgap(10);
        inputArea.setPadding(new Insets(15));
        inputArea.setStyle(themeManager.getAccent2BackgroundStyle() + " -fx-border-radius: 8; -fx-background-radius: 8;");
        
        // Command Type Selector
        Label typeLabel = new Label("Command Type:");
        typeLabel.setStyle(themeManager.getPrimaryTextStyle());
        commandTypeSelector = new ComboBox<>();
        commandTypeSelector.getItems().addAll(
            "ASSIGNMENT", "INCREASE", "DECREASE", 
            "JUMP_NOT_ZERO", "GOTO_LABEL", "LABEL"
        );
        commandTypeSelector.setValue("ASSIGNMENT");
        commandTypeSelector.setStyle(themeManager.getComboBoxStyle());
        commandTypeSelector.setOnAction(e -> onCommandTypeChanged());
        
        // Variable Field
        Label varLabel = new Label("Variable:");
        varLabel.setStyle(themeManager.getPrimaryTextStyle());
        variableField = new TextField();
        variableField.setPromptText("Enter variable name (e.g., X1, Y, Z)");
        variableField.setStyle(themeManager.getTextInputStyle());
        
        // Value Field
        Label valueLabel = new Label("Value/Label:");
        valueLabel.setStyle(themeManager.getPrimaryTextStyle());
        valueField = new TextField();
        valueField.setPromptText("Enter value or label");
        valueField.setStyle(themeManager.getTextInputStyle());
        
        // Add Instruction Button
        addInstructionBtn = new Button("➕ Add Instruction");
        addInstructionBtn.setStyle(themeManager.getSuccessButtonStyle());
        addInstructionBtn.setOnAction(e -> addInstruction());
        
        // Layout input components
        inputArea.add(typeLabel, 0, 0);
        inputArea.add(commandTypeSelector, 1, 0);
        inputArea.add(varLabel, 0, 1);
        inputArea.add(variableField, 1, 1);
        inputArea.add(valueLabel, 2, 1);
        inputArea.add(valueField, 3, 1);
        inputArea.add(addInstructionBtn, 4, 0, 1, 2);
        
        // Make columns expandable
        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        ColumnConstraints col3 = new ColumnConstraints();
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setHgrow(Priority.ALWAYS);
        ColumnConstraints col5 = new ColumnConstraints();
        inputArea.getColumnConstraints().addAll(col1, col2, col3, col4, col5);
        
        topPanel.getChildren().addAll(titleLabel, subtitleLabel, inputArea);
        mainLayout.setTop(topPanel);
    }
    
    private void createCenterPanel(BorderPane mainLayout) {
        VBox centerPanel = new VBox(10);
        centerPanel.setPadding(new Insets(20));
        centerPanel.setStyle(themeManager.getSecondaryBackgroundStyle());
        
        // Instructions table title
        Label tableTitle = new Label("📋 Program Instructions");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;" + themeManager.getPrimaryTextStyle());
        
        // Create instructions table
        instructionsTable = new TableView<>();
        instructionsTable.setStyle(themeManager.getAccent2BackgroundStyle());
        setupInstructionsTable();
        
        // Table controls
        HBox tableControls = new HBox(10);
        tableControls.setAlignment(Pos.CENTER_LEFT);
        
        editInstructionBtn = new Button("✏️ Edit Selected");
        editInstructionBtn.setStyle(themeManager.getWarningButtonStyle());
        editInstructionBtn.setDisable(true);
        editInstructionBtn.setOnAction(e -> editSelectedInstruction());
        
        deleteInstructionBtn = new Button("🗑️ Delete Selected");
        deleteInstructionBtn.setStyle(themeManager.getSecondaryButtonStyle());
        deleteInstructionBtn.setDisable(true);
        deleteInstructionBtn.setOnAction(e -> deleteSelectedInstruction());
        
        Button moveUpBtn = new Button("⬆️ Move Up");
        moveUpBtn.setStyle(themeManager.getInfoButtonStyle());
        moveUpBtn.setOnAction(e -> moveInstructionUp());
        
        Button moveDownBtn = new Button("⬇️ Move Down");
        moveDownBtn.setStyle(themeManager.getInfoButtonStyle());
        moveDownBtn.setOnAction(e -> moveInstructionDown());
        
        Button clearAllBtn = new Button("🧹 Clear All");
        clearAllBtn.setStyle(themeManager.getSecondaryButtonStyle());
        clearAllBtn.setOnAction(e -> clearAllInstructions());
        
        tableControls.getChildren().addAll(
            editInstructionBtn, deleteInstructionBtn, 
            new Separator(), moveUpBtn, moveDownBtn,
            new Separator(), clearAllBtn
        );
        
        // Selection listener for table
        instructionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            editInstructionBtn.setDisable(!hasSelection);
            deleteInstructionBtn.setDisable(!hasSelection);
        });
        
        centerPanel.getChildren().addAll(tableTitle, instructionsTable, tableControls);
        VBox.setVgrow(instructionsTable, Priority.ALWAYS);
        
        mainLayout.setCenter(centerPanel);
    }
    
    private void createBottomPanel(BorderPane mainLayout) {
        VBox bottomPanel = new VBox(10);
        bottomPanel.setPadding(new Insets(20));
        bottomPanel.setStyle(themeManager.getAccent1BackgroundStyle());
        
        // Action buttons
        HBox actionButtons = new HBox(15);
        actionButtons.setAlignment(Pos.CENTER);
        
        Button newProgramBtn = new Button("📄 New Program");
        newProgramBtn.setStyle(themeManager.getInfoButtonStyle());
        newProgramBtn.setOnAction(e -> newProgram());
        
        Button loadProgramBtn = new Button("📂 Load Program");
        loadProgramBtn.setStyle(themeManager.getLightButtonStyle());
        loadProgramBtn.setOnAction(e -> loadProgram());
        
        saveBtn = new Button("💾 Save Program");
        saveBtn.setStyle(themeManager.getSuccessButtonStyle());
        saveBtn.setOnAction(e -> saveProgram());
        
        Button saveAsBtn = new Button("💾 Save As...");
        saveAsBtn.setStyle(themeManager.getSuccessButtonStyle());
        saveAsBtn.setOnAction(e -> saveAsProgram());
        
        loadIntoEmulatorBtn = new Button("🚀 Load into Emulator");
        loadIntoEmulatorBtn.setStyle(themeManager.getSkyBlueButtonStyle());
        loadIntoEmulatorBtn.setOnAction(e -> loadIntoEmulator());
        
        Button validateBtn = new Button("✅ Validate Program");
        validateBtn.setStyle(themeManager.getWarningButtonStyle());
        validateBtn.setOnAction(e -> validateProgram());
        
        actionButtons.getChildren().addAll(
            newProgramBtn, loadProgramBtn, saveBtn, saveAsBtn,
            new Separator(), loadIntoEmulatorBtn, validateBtn
        );
        
        // Status label
        statusLabel = new Label("Ready");
        statusLabel.setStyle(themeManager.getAccent8BackgroundStyle() + " -fx-padding: 8; -fx-border-radius: 4; -fx-background-radius: 4;");
        
        bottomPanel.getChildren().addAll(actionButtons, statusLabel);
        mainLayout.setBottom(bottomPanel);
    }
    
    private void setupInstructionsTable() {
        // Line Number Column
        TableColumn<InstructionRow, Integer> lineCol = new TableColumn<>("Line");
        lineCol.setCellValueFactory(new PropertyValueFactory<>("lineNumber"));
        lineCol.setMinWidth(50);
        lineCol.setMaxWidth(70);
        
        // Type Column
        TableColumn<InstructionRow, String> typeCol = new TableColumn<>("Command Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setMinWidth(120);
        
        // Variable Column
        TableColumn<InstructionRow, String> varCol = new TableColumn<>("Variable");
        varCol.setCellValueFactory(new PropertyValueFactory<>("variable"));
        varCol.setMinWidth(80);
        
        // Value Column
        TableColumn<InstructionRow, String> valueCol = new TableColumn<>("Value/Label");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueCol.setMinWidth(100);
        
        // Description Column
        TableColumn<InstructionRow, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescription()));
        descCol.setMinWidth(200);
        
        instructionsTable.getColumns().addAll(lineCol, typeCol, varCol, valueCol, descCol);
        
        // Make table fill available space
        instructionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
    
    private void onCommandTypeChanged() {
        String selectedType = commandTypeSelector.getValue();
        
        // Update field prompts based on command type
        switch (selectedType) {
            case "ASSIGNMENT":
                variableField.setPromptText("Variable name (e.g., X1, Y)");
                valueField.setPromptText("Value or expression");
                variableField.setDisable(false);
                valueField.setDisable(false);
                break;
            case "INCREASE":
            case "DECREASE":
                variableField.setPromptText("Variable name to " + selectedType.toLowerCase());
                valueField.setPromptText("(not used)");
                variableField.setDisable(false);
                valueField.setDisable(true);
                valueField.clear();
                break;
            case "JUMP_NOT_ZERO":
                variableField.setPromptText("Variable to test");
                valueField.setPromptText("Label to jump to");
                variableField.setDisable(false);
                valueField.setDisable(false);
                break;
            case "GOTO_LABEL":
                variableField.setPromptText("Variable (required for S-Variable)");
                valueField.setPromptText("Label to go to");
                variableField.setDisable(false);
                valueField.setDisable(false);
                break;
            case "LABEL":
                variableField.setPromptText("(not used)");
                valueField.setPromptText("Label name");
                variableField.setDisable(true);
                variableField.clear();
                valueField.setDisable(false);
                break;
        }
    }
    
    private void addInstruction() {
        String type = commandTypeSelector.getValue();
        String variable = variableField.getText().trim();
        String value = valueField.getText().trim();
        
        // Validate input
        if (!validateInstructionInput(type, variable, value)) {
            return;
        }
        
        // Create new instruction
        InstructionRow newInstruction = new InstructionRow(
            instructions.size() + 1, type, variable, value
        );
        
        instructions.add(newInstruction);
        refreshInstructionsTable();
        
        // Clear input fields
        variableField.clear();
        valueField.clear();
        
        updateStatus("Added " + type + " instruction");
    }
    
    private boolean validateInstructionInput(String type, String variable, String value) {
        switch (type) {
            case "ASSIGNMENT":
                if (variable.isEmpty() || value.isEmpty()) {
                    showError("Assignment requires both variable and value");
                    return false;
                }
                break;
            case "INCREASE":
            case "DECREASE":
                if (variable.isEmpty()) {
                    showError(type + " requires a variable name");
                    return false;
                }
                break;
            case "JUMP_NOT_ZERO":
                if (variable.isEmpty() || value.isEmpty()) {
                    showError("Jump instruction requires both variable and label");
                    return false;
                }
                break;
            case "GOTO_LABEL":
                if (variable.isEmpty()) {
                    showError("GOTO_LABEL requires a variable (for S-Variable element)");
                    return false;
                }
                if (value.isEmpty()) {
                    showError("GOTO_LABEL requires a label name");
                    return false;
                }
                break;
            case "LABEL":
                if (value.isEmpty()) {
                    showError("LABEL requires a label name");
                    return false;
                }
                break;
        }
        return true;
    }
    
    private void editSelectedInstruction() {
        InstructionRow selected = instructionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        // Populate fields with selected instruction
        commandTypeSelector.setValue(selected.getType());
        variableField.setText(selected.getVariable());
        valueField.setText(selected.getValue());
        
        // Remove the instruction (will be re-added when user clicks Add)
        instructions.remove(selected);
        refreshInstructionsTable();
        
        updateStatus("Editing instruction - modify and click Add to update");
    }
    
    private void deleteSelectedInstruction() {
        InstructionRow selected = instructionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        instructions.remove(selected);
        refreshInstructionsTable();
        updateStatus("Deleted instruction");
    }
    
    private void moveInstructionUp() {
        InstructionRow selected = instructionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        int index = instructions.indexOf(selected);
        if (index > 0) {
            instructions.remove(index);
            instructions.add(index - 1, selected);
            refreshInstructionsTable();
            instructionsTable.getSelectionModel().select(index - 1);
        }
    }
    
    private void moveInstructionDown() {
        InstructionRow selected = instructionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        int index = instructions.indexOf(selected);
        if (index < instructions.size() - 1) {
            instructions.remove(index);
            instructions.add(index + 1, selected);
            refreshInstructionsTable();
            instructionsTable.getSelectionModel().select(index + 1);
        }
    }
    
    private void clearAllInstructions() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear All Instructions");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("This will delete all instructions in the current program.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            instructions.clear();
            refreshInstructionsTable();
            updateStatus("Cleared all instructions");
        }
    }
    
    private void refreshInstructionsTable() {
        // Update line numbers
        for (int i = 0; i < instructions.size(); i++) {
            instructions.get(i).setLineNumber(i + 1);
        }
        
        instructionsTable.getItems().clear();
        instructionsTable.getItems().addAll(instructions);
    }
    
    private void newProgram() {
        if (!instructions.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("New Program");
            confirm.setHeaderText("Current program will be lost");
            confirm.setContentText("Are you sure you want to create a new program?");
            
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }
        
        instructions.clear();
        currentFile = null;
        refreshInstructionsTable();
        updateStatus("Started new program");
        editorStage.setTitle("Diligent Program Editor 🦫 - New Program");
    }
    
    private void loadProgram() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load S-Emulator Program");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("S-Emulator Programs", "*.xml")
        );
        
        File file = fileChooser.showOpenDialog(editorStage);
        if (file != null) {
            loadFromXMLFile(file);
        }
    }
    
    private void loadFromXMLFile(File file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            
            // Clear current instructions
            instructions.clear();
            
            // Parse S-Instructions
            NodeList instructionNodes = doc.getElementsByTagName("S-Instruction");
            System.out.println("=== XML LOADING DEBUG ===");
            System.out.println("Found " + instructionNodes.getLength() + " S-Instruction nodes");
            
            for (int i = 0; i < instructionNodes.getLength(); i++) {
                Element instruction = (Element) instructionNodes.item(i);
                String type = instruction.getAttribute("type");
                String name = instruction.getAttribute("name");
                
                System.out.println("Instruction " + (i+1) + ": type=" + type + ", name=" + name);
                
                // Get variable if present
                String variable = "";
                NodeList variableNodes = instruction.getElementsByTagName("S-Variable");
                if (variableNodes.getLength() > 0) {
                    variable = variableNodes.item(0).getTextContent().trim();
                    System.out.println("  Variable: '" + variable + "'");
                }
                
                // Get arguments if present
                String value = "";
                NodeList argNodes = instruction.getElementsByTagName("S-Instruction-Argument");
                for (int j = 0; j < argNodes.getLength(); j++) {
                    Element arg = (Element) argNodes.item(j);
                    String argName = arg.getAttribute("name");
                    String argValue = arg.getAttribute("value");
                    
                    System.out.println("  Argument: " + argName + " = " + argValue);
                    
                    // Map argument names to values based on instruction type
                    if ("assignedVariable".equals(argName) || "JNZLabel".equals(argName) || "label".equals(argName)) {
                        value = argValue;
                    }
                }
                
                System.out.println("  Final values: variable='" + variable + "', value='" + value + "'");
                
                // Create instruction row based on name
                InstructionRow row = new InstructionRow(i + 1, name, variable, value);
                
                instructions.add(row);
            }
            
            // Refresh table
            refreshInstructionsTable();
            updateStatus("Loaded " + instructions.size() + " instructions from " + file.getName());
            currentFile = file;
            
        } catch (Exception e) {
            showError("Failed to load file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveProgram() {
        if (currentFile == null) {
            saveAsProgram();
        } else {
            saveToFile(currentFile);
        }
    }
    
    private void saveAsProgram() {
        if (instructions.isEmpty()) {
            showError("Cannot save empty program");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save S-Emulator Program");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("S-Emulator Programs", "*.xml")
        );
        fileChooser.setInitialFileName("my_program.xml");
        
        File file = fileChooser.showSaveDialog(editorStage);
        if (file != null) {
            saveToFile(file);
        }
    }
    
    private void saveToFile(File file) {
        try {
            String xml = generateXML();
            
            // Debug output to see the generated XML
            System.out.println("=== GENERATED XML DEBUG ===");
            System.out.println(xml);
            System.out.println("=== END XML DEBUG ===");
            
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(xml);
            }
            
            currentFile = file;
            updateStatus("Saved program to " + file.getName());
            editorStage.setTitle("Diligent Program Editor 🦫 - " + file.getName());
            
        } catch (IOException e) {
            showError("Failed to save program: " + e.getMessage());
        }
    }
    
    private String generateXML() {
        if (instructions.isEmpty()) {
            return null;
        }
        
        // Create variable mapping for user-friendly names to S-program format
        Map<String, String> variableMapping = createVariableMapping();
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<S-Program name=\"DiligentProgram\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"S-Emulator-v2.xsd\">\n");
        xml.append("    <S-Instructions>\n");
        
        // Process instructions, handling labels specially
        for (int i = 0; i < instructions.size(); i++) {
            InstructionRow instruction = instructions.get(i);
            
            // Skip LABEL instructions - they will be embedded in the next instruction
            if ("LABEL".equals(instruction.getType())) {
                continue;
            }
            
            // Check if the previous instruction was a LABEL that should be embedded here
            String labelToEmbed = null;
            if (i > 0 && "LABEL".equals(instructions.get(i - 1).getType())) {
                labelToEmbed = instructions.get(i - 1).getValue();
            }
            
            switch (instruction.getType()) {
                case "ASSIGNMENT":
                    String assignmentValue = mapVariableValue(instruction.getValue(), variableMapping);
                    boolean isLiteralValue = assignmentValue.matches("\\d+(\\.\\d+)?");
                    
                    if (isLiteralValue) {
                        // Use CONSTANT_ASSIGNMENT for literal values
                        xml.append("        <S-Instruction type=\"synthetic\" name=\"CONSTANT_ASSIGNMENT\">\n");
                        xml.append("            <S-Variable>").append(escapeXml(mapVariable(instruction.getVariable(), variableMapping))).append("</S-Variable>\n");
                        if (labelToEmbed != null) {
                            xml.append("            <S-Label>").append(escapeXml(labelToEmbed)).append("</S-Label>\n");
                        }
                        xml.append("            <S-Instruction-Arguments>\n");
                        xml.append("                <S-Instruction-Argument name=\"constantValue\" value=\"").append(escapeXml(assignmentValue)).append("\"/>\n");
                        xml.append("            </S-Instruction-Arguments>\n");
                    } else {
                        // Use ASSIGNMENT for variable references
                        xml.append("        <S-Instruction type=\"synthetic\" name=\"ASSIGNMENT\">\n");
                        xml.append("            <S-Variable>").append(escapeXml(mapVariable(instruction.getVariable(), variableMapping))).append("</S-Variable>\n");
                        if (labelToEmbed != null) {
                            xml.append("            <S-Label>").append(escapeXml(labelToEmbed)).append("</S-Label>\n");
                        }
                        xml.append("            <S-Instruction-Arguments>\n");
                        xml.append("                <S-Instruction-Argument name=\"assignedVariable\" value=\"").append(escapeXml(assignmentValue)).append("\"/>\n");
                        xml.append("            </S-Instruction-Arguments>\n");
                    }
                    xml.append("        </S-Instruction>\n");
                    break;
                    
                case "INCREASE":
                    xml.append("        <S-Instruction type=\"basic\" name=\"INCREASE\">\n");
                    xml.append("            <S-Variable>").append(escapeXml(mapVariable(instruction.getVariable(), variableMapping))).append("</S-Variable>\n");
                    if (labelToEmbed != null) {
                        xml.append("            <S-Label>").append(escapeXml(labelToEmbed)).append("</S-Label>\n");
                    }
                    xml.append("        </S-Instruction>\n");
                    break;
                    
                case "DECREASE":
                    xml.append("        <S-Instruction type=\"basic\" name=\"DECREASE\">\n");
                    xml.append("            <S-Variable>").append(escapeXml(mapVariable(instruction.getVariable(), variableMapping))).append("</S-Variable>\n");
                    if (labelToEmbed != null) {
                        xml.append("            <S-Label>").append(escapeXml(labelToEmbed)).append("</S-Label>\n");
                    }
                    xml.append("        </S-Instruction>\n");
                    break;
                    
                case "JUMP_NOT_ZERO":
                    xml.append("        <S-Instruction type=\"basic\" name=\"JUMP_NOT_ZERO\">\n");
                    xml.append("            <S-Variable>").append(escapeXml(mapVariable(instruction.getVariable(), variableMapping))).append("</S-Variable>\n");
                    if (labelToEmbed != null) {
                        xml.append("            <S-Label>").append(escapeXml(labelToEmbed)).append("</S-Label>\n");
                    }
                    xml.append("            <S-Instruction-Arguments>\n");
                    xml.append("                <S-Instruction-Argument name=\"JNZLabel\" value=\"").append(escapeXml(instruction.getValue())).append("\"/>\n");
                    xml.append("            </S-Instruction-Arguments>\n");
                    xml.append("        </S-Instruction>\n");
                    break;
                    
                case "GOTO_LABEL":
                    xml.append("        <S-Instruction type=\"synthetic\" name=\"GOTO_LABEL\">\n");
                    xml.append("            <S-Variable>").append(escapeXml(mapVariable(instruction.getVariable(), variableMapping))).append("</S-Variable>\n");
                    if (labelToEmbed != null) {
                        xml.append("            <S-Label>").append(escapeXml(labelToEmbed)).append("</S-Label>\n");
                    }
                    xml.append("            <S-Instruction-Arguments>\n");
                    xml.append("                <S-Instruction-Argument name=\"gotoLabel\" value=\"").append(escapeXml(instruction.getValue())).append("\"/>\n");
                    xml.append("            </S-Instruction-Arguments>\n");
                    xml.append("        </S-Instruction>\n");
                    break;
            }
        }
        
        xml.append("    </S-Instructions>\n");
        xml.append("</S-Program>\n");
        
        return xml.toString();
    }
    
    private Map<String, String> createVariableMapping() {
        // Collect all unique variable names used in the program
        Set<String> userVariables = new HashSet<>();
        for (InstructionRow instruction : instructions) {
            String var = instruction.getVariable();
            if (var != null && !var.trim().isEmpty()) {
                userVariables.add(var.trim());
            }
            // Also check values that might be variable references
            String val = instruction.getValue();
            if (val != null && !val.trim().isEmpty() && !val.matches("\\d+")) {
                // If it's not a number, it might be a variable reference
                userVariables.add(val.trim());
            }
        }
        
        // Create mapping from user-friendly names to S-program format
        Map<String, String> mapping = new HashMap<>();
        // Preserve any user-entered xN variables (input variables) and 'y' if present.
        // Only map internal/non-x variables to zN to avoid rewriting inputs.
        List<String> sortedVars = new ArrayList<>(userVariables);
        Collections.sort(sortedVars); // Sort for consistency

        // Determine which variables to preserve (inputs and explicit 'y')
        Set<String> preserved = new HashSet<>();
        for (String v : sortedVars) {
            if (v.matches("(?i)^x\\d+$") || v.equals("y") || v.equals("Y")) {
                preserved.add(v);
            }
        }

        // Look for a result-like variable to map to 'y', but only among non-preserved vars
        String resultVar = null;
        for (String var : sortedVars) {
            if (preserved.contains(var)) continue;
            String low = var.toLowerCase();
            if (low.contains("result") || low.contains("output") || low.contains("sum") || low.contains("total")) {
                resultVar = var;
                break;
            }
        }

        // If no obvious result variable, pick the last non-preserved var (if any)
        if (resultVar == null) {
            for (int i = sortedVars.size() - 1; i >= 0; i--) {
                String var = sortedVars.get(i);
                if (!preserved.contains(var)) {
                    resultVar = var;
                    break;
                }
            }
        }

        int zIndex = 1;
        for (String userVar : sortedVars) {
            if (preserved.contains(userVar)) {
                // keep as-is; don't add a mapping so mapVariable will return the original
                continue;
            }
            if (userVar.equals(resultVar)) {
                mapping.put(userVar, "y"); // Main result variable (if chosen)
            } else {
                mapping.put(userVar, "z" + zIndex); // Internal working variables
                zIndex++;
            }
        }
        
        System.out.println("=== VARIABLE MAPPING ===");
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
        
        return mapping;
    }
    
    private String mapVariable(String variable, Map<String, String> mapping) {
        if (variable == null || variable.trim().isEmpty()) {
            return "";
        }
        String trimmed = variable.trim();
        return mapping.getOrDefault(trimmed, trimmed);
    }
    
    private String mapVariableValue(String value, Map<String, String> mapping) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String trimmed = value.trim();
        // If it's a number, return as-is
        if (trimmed.matches("\\d+")) {
            return trimmed;
        }
        // Otherwise, try to map it as a variable
        return mapping.getOrDefault(trimmed, trimmed);
    }
    
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
    
    private void loadIntoEmulator() {
        if (instructions.isEmpty()) {
            showError("Cannot load empty program into emulator");
            return;
        }
        
        try {
            // Create temporary file
            File tempFile = File.createTempFile("diligent_program_", ".xml");
            tempFile.deleteOnExit();
            
            // Save current program to temp file
            saveToFile(tempFile);
            
            // Load into parent emulator
            if (parentApp != null) {
                parentApp.loadProgramFile(tempFile);
                updateStatus("Program loaded into emulator successfully!");
                
                // Optionally close the editor
                Platform.runLater(() -> {
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Program Loaded");
                    info.setHeaderText("Success!");
                    info.setContentText("Your program has been loaded into the S-Emulator and is ready to run.");
                    info.showAndWait();
                });
            }
            
        } catch (IOException e) {
            showError("Failed to load program into emulator: " + e.getMessage());
        }
    }
    
    private void validateProgram() {
        if (instructions.isEmpty()) {
            showError("No instructions to validate");
            return;
        }
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Check for basic syntax errors
        for (InstructionRow instruction : instructions) {
            switch (instruction.getType()) {
                case "ASSIGNMENT":
                    if (instruction.getVariable().isEmpty()) {
                        errors.add("Line " + instruction.getLineNumber() + ": Assignment missing variable");
                    }
                    if (instruction.getValue().isEmpty()) {
                        errors.add("Line " + instruction.getLineNumber() + ": Assignment missing value");
                    }
                    break;
                case "INCREASE":
                case "DECREASE":
                    if (instruction.getVariable().isEmpty()) {
                        errors.add("Line " + instruction.getLineNumber() + ": " + instruction.getType() + " missing variable");
                    }
                    break;
                case "JUMP_NOT_ZERO":
                    if (instruction.getVariable().isEmpty()) {
                        errors.add("Line " + instruction.getLineNumber() + ": Jump instruction missing variable");
                    }
                    if (instruction.getValue().isEmpty()) {
                        errors.add("Line " + instruction.getLineNumber() + ": Jump instruction missing label");
                    }
                    break;
                case "GOTO_LABEL":
                    if (instruction.getVariable().isEmpty()) {
                        errors.add("Line " + instruction.getLineNumber() + ": GOTO_LABEL missing variable (required for S-Variable)");
                    }
                    if (instruction.getValue().isEmpty()) {
                        errors.add("Line " + instruction.getLineNumber() + ": " + instruction.getType() + " missing label");
                    }
                    break;
                case "LABEL":
                    if (instruction.getValue().isEmpty()) {
                        errors.add("Line " + instruction.getLineNumber() + ": " + instruction.getType() + " missing label");
                    }
                    break;
            }
        }
        
        // Check for undefined labels
        List<String> definedLabels = new ArrayList<>();
        List<String> usedLabels = new ArrayList<>();
        
        for (InstructionRow instruction : instructions) {
            if ("LABEL".equals(instruction.getType())) {
                definedLabels.add(instruction.getValue());
            } else if ("JUMP_NOT_ZERO".equals(instruction.getType()) || "GOTO_LABEL".equals(instruction.getType())) {
                usedLabels.add(instruction.getValue());
            }
        }
        
        for (String usedLabel : usedLabels) {
            if (!definedLabels.contains(usedLabel)) {
                errors.add("Undefined label: " + usedLabel);
            }
        }
        
        for (String definedLabel : definedLabels) {
            if (!usedLabels.contains(definedLabel)) {
                warnings.add("Unused label: " + definedLabel);
            }
        }
        
        // Show validation results
        if (errors.isEmpty() && warnings.isEmpty()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Validation Complete");
            info.setHeaderText("Program is valid! ✅");
            info.setContentText("No errors or warnings found. Your program is ready to run.");
            info.showAndWait();
            updateStatus("Program validation passed");
        } else {
            StringBuilder message = new StringBuilder();
            
            if (!errors.isEmpty()) {
                message.append("ERRORS:\n");
                for (String error : errors) {
                    message.append("• ").append(error).append("\n");
                }
                message.append("\n");
            }
            
            if (!warnings.isEmpty()) {
                message.append("WARNINGS:\n");
                for (String warning : warnings) {
                    message.append("• ").append(warning).append("\n");
                }
            }
            
            Alert alert = new Alert(errors.isEmpty() ? Alert.AlertType.WARNING : Alert.AlertType.ERROR);
            alert.setTitle("Validation Results");
            alert.setHeaderText(errors.isEmpty() ? "Warnings Found" : "Errors Found");
            alert.setContentText(message.toString());
            alert.showAndWait();
            
            updateStatus("Validation found " + errors.size() + " errors, " + warnings.size() + " warnings");
        }
    }
    
    private void applyTheme() {
        // This will be called when theme changes
        // For now, the theme is applied during initialization
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
        Platform.runLater(() -> {
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> statusLabel.setText("Ready"))
            );
            timeline.play();
        });
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Program Editor Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void show() {
        editorStage.show();
        editorStage.toFront();
    }
    
    public void close() {
        editorStage.close();
    }
}