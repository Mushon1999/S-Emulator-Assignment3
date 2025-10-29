package se.emulator.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.scene.layout.*;
import se.emulator.server.*;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * Program management screen for uploading and viewing programs.
 */
public class ProgramManagementScreen {
    private final SEMulatorClientApp app;
    private final VBox content;
    private String currentUser;
    
    // UI Components
    private TableView<ProgramInfo> programsTable;
    private TableView<FunctionInfo> functionsTable;
    private TableView<User> usersTable;
    private Button uploadButton;
    private Button selectProgramButton;
    private Button selectFunctionButton;
    private Label userCreditsLabel;
    
    public ProgramManagementScreen(SEMulatorClientApp app) {
        this.app = app;
        this.content = createContent();
    }
    
    private VBox createContent() {
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        
        // User info section
        HBox userInfoBox = new HBox(20);
        userCreditsLabel = new Label("Credits: Loading...");
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> app.logoutUser());
        userInfoBox.getChildren().addAll(userCreditsLabel, logoutButton);
        
        // Programs section
        VBox programsSection = createProgramsSection();
        
        // Functions section
        VBox functionsSection = createFunctionsSection();
        
        // Users section
        VBox usersSection = createUsersSection();
        
        // Main layout with tabs
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
            new Tab("Programs", programsSection),
            new Tab("Functions", functionsSection),
            new Tab("Users", usersSection)
        );
        
        mainContainer.getChildren().addAll(userInfoBox, tabPane);
        return mainContainer;
    }
    
    private VBox createProgramsSection() {
        VBox container = new VBox(10);
        
        // Programs table
        programsTable = new TableView<>();
        programsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<ProgramInfo, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        TableColumn<ProgramInfo, String> uploadedByCol = new TableColumn<>("Uploaded By");
        uploadedByCol.setCellValueFactory(new PropertyValueFactory<>("uploadedBy"));
        
        TableColumn<ProgramInfo, Integer> instructionsCol = new TableColumn<>("Instructions");
        instructionsCol.setCellValueFactory(new PropertyValueFactory<>("instructionCount"));
        
        TableColumn<ProgramInfo, Integer> maxLevelCol = new TableColumn<>("Max Level");
        maxLevelCol.setCellValueFactory(new PropertyValueFactory<>("maxExecutionLevel"));
        
        TableColumn<ProgramInfo, Integer> executionsCol = new TableColumn<>("Executions");
        executionsCol.setCellValueFactory(new PropertyValueFactory<>("executionCount"));
        
        TableColumn<ProgramInfo, Double> avgCostCol = new TableColumn<>("Avg Cost");
        avgCostCol.setCellValueFactory(new PropertyValueFactory<>("averageCreditCost"));
        
        programsTable.getColumns().addAll(nameCol, uploadedByCol, instructionsCol, 
                                           maxLevelCol, executionsCol, avgCostCol);
        
        // Upload section
        HBox uploadBox = new HBox(10);
        uploadButton = new Button("Upload Program");
        uploadButton.setOnAction(e -> handleUploadProgram());
        uploadBox.getChildren().add(uploadButton);
        
        // Selection button
        selectProgramButton = new Button("Execute Selected Program");
        selectProgramButton.setDisable(true);
        selectProgramButton.setOnAction(e -> handleSelectProgram());
        
        programsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectProgramButton.setDisable(newVal == null);
        });
        
        container.getChildren().addAll(
            new Label("Available Programs:"),
            programsTable,
            uploadBox,
            selectProgramButton
        );
        
        return container;
    }
    
    private VBox createFunctionsSection() {
        VBox container = new VBox(10);
        
        // Functions table
        functionsTable = new TableView<>();
        functionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<FunctionInfo, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        TableColumn<FunctionInfo, String> userStringCol = new TableColumn<>("User String");
        userStringCol.setCellValueFactory(new PropertyValueFactory<>("userString"));
        
        TableColumn<FunctionInfo, String> programCol = new TableColumn<>("Program");
        programCol.setCellValueFactory(new PropertyValueFactory<>("programName"));
        
        TableColumn<FunctionInfo, String> uploadedByCol = new TableColumn<>("Uploaded By");
        uploadedByCol.setCellValueFactory(new PropertyValueFactory<>("uploadedBy"));
        
        TableColumn<FunctionInfo, Integer> instructionsCol = new TableColumn<>("Instructions");
        instructionsCol.setCellValueFactory(new PropertyValueFactory<>("instructionCount"));
        
        functionsTable.getColumns().addAll(nameCol, userStringCol, programCol, uploadedByCol, instructionsCol);
        
        // Selection button
        selectFunctionButton = new Button("Execute Selected Function");
        selectFunctionButton.setDisable(true);
        selectFunctionButton.setOnAction(e -> handleSelectFunction());
        
        functionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectFunctionButton.setDisable(newVal == null);
        });
        
        container.getChildren().addAll(
            new Label("Available Functions:"),
            functionsTable,
            selectFunctionButton
        );
        
        return container;
    }
    
    private VBox createUsersSection() {
        VBox container = new VBox(10);
        
        // Users table
        usersTable = new TableView<>();
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<User, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        TableColumn<User, Long> creditsCol = new TableColumn<>("Credits");
        creditsCol.setCellValueFactory(new PropertyValueFactory<>("credits"));
        
        TableColumn<User, Integer> programsCol = new TableColumn<>("Programs");
        programsCol.setCellValueFactory(new PropertyValueFactory<>("mainProgramsCount"));
        
        TableColumn<User, Integer> functionsCol = new TableColumn<>("Functions");
        functionsCol.setCellValueFactory(new PropertyValueFactory<>("helperFunctionsCount"));
        
        TableColumn<User, Long> totalUsedCol = new TableColumn<>("Total Used");
        totalUsedCol.setCellValueFactory(new PropertyValueFactory<>("totalCreditsUsed"));
        
        TableColumn<User, Integer> executionsCol = new TableColumn<>("Executions");
        executionsCol.setCellValueFactory(new PropertyValueFactory<>("executionsPerformed"));
        
        usersTable.getColumns().addAll(nameCol, creditsCol, programsCol, functionsCol, totalUsedCol, executionsCol);
        
        container.getChildren().addAll(
            new Label("Connected Users:"),
            usersTable
        );
        
        return container;
    }
    
    private void handleUploadProgram() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select XML Program File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("XML Files", "*.xml")
        );
        
        File selectedFile = fileChooser.showOpenDialog(app.getPrimaryStage());
        if (selectedFile != null) {
            try {
                String xmlContent = Files.readString(selectedFile.toPath());
                boolean success = app.getServerService().uploadProgram(currentUser, xmlContent);
                
                if (success) {
                    app.showAlert("Upload Successful", "Program uploaded successfully!");
                    updateData();
                } else {
                    app.showErrorAlert("Upload Failed", "Failed to upload program. Check if program name already exists or if the XML is valid.");
                }
            } catch (Exception e) {
                app.showErrorAlert("Upload Error", "Error reading file: " + e.getMessage());
            }
        }
    }
    
    private void handleSelectProgram() {
        ProgramInfo selected = programsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            app.getExecutionScreen().setSelectedProgram(selected.getName(), true);
            app.getMainTabPane().getSelectionModel().select(2); // Switch to execution tab
        }
    }
    
    private void handleSelectFunction() {
        FunctionInfo selected = functionsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            app.getExecutionScreen().setSelectedProgram(selected.getName(), false);
            app.getMainTabPane().getSelectionModel().select(2); // Switch to execution tab
        }
    }
    
    public void setCurrentUser(String username) {
        this.currentUser = username;
        updateData();
    }
    
    public void updateData() {
        Platform.runLater(() -> {
            // Update user credits
            if (currentUser != null) {
                List<User> users = app.getServerService().getAllUsers();
                User currentUserObj = users.stream()
                    .filter(u -> u.getName().equals(currentUser))
                    .findFirst()
                    .orElse(null);
                if (currentUserObj != null) {
                    userCreditsLabel.setText("Credits: " + currentUserObj.getCredits());
                }
            }
            
            // Update programs table
            List<ProgramInfo> programs = app.getServerService().getAllPrograms();
            programsTable.setItems(FXCollections.observableArrayList(programs));
            
            // Update functions table
            List<FunctionInfo> functions = app.getServerService().getAllFunctions();
            functionsTable.setItems(FXCollections.observableArrayList(functions));
            
            // Update users table
            List<User> users = app.getServerService().getAllUsers();
            usersTable.setItems(FXCollections.observableArrayList(users));
        });
    }
    
    public void clearData() {
        programsTable.setItems(FXCollections.emptyObservableList());
        functionsTable.setItems(FXCollections.emptyObservableList());
        usersTable.setItems(FXCollections.emptyObservableList());
        userCreditsLabel.setText("Credits: Not logged in");
    }
    
    public VBox getContent() {
        return content;
    }
}
