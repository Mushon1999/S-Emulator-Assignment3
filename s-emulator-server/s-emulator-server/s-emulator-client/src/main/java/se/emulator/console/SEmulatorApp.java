package se.emulator.console;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import se.emulator.engine.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * S-Emulator JavaFX GUI Application
 * Matches the exact layout from the provided design picture
 */
public class SEmulatorApp extends Application {

    private EngineService engine;
    private Stage primaryStage;
    private ThemeManager themeManager;
    private ProgramEditor programEditor; // Diligent Program Editor instance

    // UI Components matching the picture layout
    private Label filePathLabel;
    private ComboBox<String> programSelector;
    private Label degreeLabel;
    private TextField highlightField;
    private TableView<InstructionRow> instructionsTable;
    private TreeTableView<ProgramTreeNode> instructionsTreeTable;
    private StackPane instructionsContainer; // Container to switch between table and tree
    private ToggleButton viewToggleBtn; // Switch between table and tree view
    private boolean isTreeViewMode = false;
    private Label summaryLabel;
    private TextArea instructionHistoryArea;
    private TextArea variablesArea;
    private TextArea executionInputsArea;
    private VBox inputVariablesBox; // For dynamic input fields
    private Map<String, TextField> inputFields = new HashMap<>(); // Track input fields
    private boolean isDebugMode = false;
    private boolean isExecuting = false;
    private int currentInstructionIndex = -1;
    private Label cyclesLabel;
    
    // Debug execution state
    private List<Instruction> debugInstructions = new ArrayList<>();
    private Map<String, Double> debugVariables = new HashMap<>();
    private List<Double> debugInputs = new ArrayList<>();
    private int debugStep = 0;
    private boolean debugPaused = false;
    private Button stepBtn, resumeBtn, stopBtn, stepBackBtn;
    private Label debugStatusLabel;
    private DebugContext currentDebugContext;
    private TableView<HistoryRow> historyTable;
    private ProgressBar loadingProgress;
    private Button runBtn; // Reference to Run button for enabling/disabling

    // Current state
    private int currentDepth = 0;
    private int maxDepth = 0;
    private String currentProgramContext = null; // null = main program, otherwise function user-string
    private String currentLoadedFileName = null; // Track the loaded file name for result calculations
    
    // Theme management
    private BorderPane mainRoot;
    private VBox topContainer;
    private VBox instructionsPanel;
    private VBox historyChainPanel;
    private VBox debuggerPanel;
    private VBox historyStatsPanel;
    private ComboBox<String> themeSelector;
    
    // Animation management
    private AnimationManager animationManager;
    private CheckBox animationsEnabledCheck;
    private ComboBox<String> animationTypeSelector;
    private CheckBox splashEnabledCheck;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.engine = new EngineServiceImpl();
        this.themeManager = new ThemeManager();
        this.animationManager = new AnimationManager();

        BorderPane root = createMainLayout();
        Scene scene = new Scene(root, 1200, 800);

        primaryStage.setTitle("S-Emulator - JavaFX Version");
        primaryStage.setScene(scene);
        
        // Make window responsive
        primaryStage.setMinWidth(800);  // Minimum width for usability
        primaryStage.setMinHeight(600); // Minimum height for usability
        primaryStage.setResizable(true); // Ensure resizable
        
        primaryStage.setOnCloseRequest(e -> {
            System.out.println("JavaFX Application closing...");
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
        
        // Play startup animation
        animationManager.playStartupAnimation(primaryStage, scene, null);

        // Ensure the application doesn't close immediately
        Platform.setImplicitExit(false);

        updateUI();
        
        // Apply initial theme
        applyCurrentTheme();

        System.out.println("JavaFX GUI started successfully!");
    }

    private BorderPane createMainLayout() {
        mainRoot = new BorderPane();
        mainRoot.setPadding(new Insets(5));
        mainRoot.setStyle(themeManager.getMainBackgroundStyle());

        // Top area with controls and progress bar
        VBox topArea = new VBox(3);
        HBox topControls = createTopControls();
        loadingProgress = new ProgressBar();
        loadingProgress.setVisible(false);
        loadingProgress.setMaxWidth(Double.MAX_VALUE);
        topArea.getChildren().addAll(topControls, loadingProgress);
        mainRoot.setTop(topArea);

        // Main content area - responsive layout without individual scrolls
        SplitPane mainHorizontalSplit = new SplitPane();
        mainHorizontalSplit.setOrientation(Orientation.HORIZONTAL);
        mainHorizontalSplit.setDividerPositions(0.6); // Initial position

        // Left side vertical split
        SplitPane leftVerticalSplit = new SplitPane();
        leftVerticalSplit.setOrientation(Orientation.VERTICAL);
        leftVerticalSplit.setMinWidth(400); // Minimum width for left side
        leftVerticalSplit.setPrefWidth(700);

        // Instructions panel (top left) - no individual scroll
        instructionsPanel = createInstructionsPanel();

        // History chain panel (bottom left) - no individual scroll
        historyChainPanel = createHistoryChainPanel();

        leftVerticalSplit.getItems().addAll(instructionsPanel, historyChainPanel);
        leftVerticalSplit.setDividerPositions(0.65);

        // Right side vertical split
        SplitPane rightVerticalSplit = new SplitPane();
        rightVerticalSplit.setOrientation(Orientation.VERTICAL);
        rightVerticalSplit.setMinWidth(300); // Minimum width for right side
        rightVerticalSplit.setPrefWidth(500);

        // Debugger/Execution panel (top right) - no individual scroll
        debuggerPanel = createExecutionPanel();

        // History/Statistics panel (bottom right) - no individual scroll
        historyStatsPanel = createHistoryStatsPanel();

        rightVerticalSplit.getItems().addAll(debuggerPanel, historyStatsPanel);
        rightVerticalSplit.setDividerPositions(0.65);

        // Add to main horizontal split
        mainHorizontalSplit.getItems().addAll(leftVerticalSplit, rightVerticalSplit);

        // Wrap the main content in a single scroll pane for responsiveness
        ScrollPane mainScrollPane = new ScrollPane(mainHorizontalSplit);
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setFitToHeight(true);
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainScrollPane.setStyle("-fx-background-color: transparent;");

        mainRoot.setCenter(mainScrollPane);

        return mainRoot;
    }

    private HBox createTopControls() {
        topContainer = new VBox(8);
        topContainer.setPadding(new Insets(8));
        topContainer.setStyle(themeManager.getAccent1BackgroundStyle());

        // First row - File operations and program selection
        HBox firstRow = new HBox(10);
        firstRow.setAlignment(Pos.CENTER_LEFT);
        
        // Load File Button (modern green style)
        Button loadFileBtn = new Button("Load File");
        loadFileBtn.setStyle(themeManager.getSuccessButtonStyle());
        loadFileBtn.setOnAction(e -> loadFile());

        // Program Editor Button (Diligent Program Editor - Busy Beaver reference!)
        Button programEditorBtn = new Button("🦫 Program Editor");
        programEditorBtn.setStyle(themeManager.getWarningButtonStyle());
        programEditorBtn.setTooltip(new Tooltip("Diligent Program Editor - Create programs graphically!"));
        programEditorBtn.setOnAction(e -> openProgramEditor());

        // Currently Loaded File path (improved styling)
        filePathLabel = new Label("Currently Loaded File path");
        filePathLabel.setStyle(themeManager.getAccent8BackgroundStyle());
        filePathLabel.setPrefWidth(300);
        filePathLabel.setMinWidth(200);
        filePathLabel.setMaxWidth(500);
        filePathLabel.setWrapText(true);

        // Program/function selector (improved styling)
        programSelector = new ComboBox<>();
        programSelector.setPromptText("Select Program");
        programSelector.setStyle(themeManager.getComboBoxStyle());
        programSelector.setPrefWidth(140);
        programSelector.setMinWidth(120);
        programSelector.setMaxWidth(200);
        programSelector.setOnAction(e -> onProgramSelectionChanged());

        firstRow.getChildren().addAll(loadFileBtn, programEditorBtn, filePathLabel, programSelector);
        HBox.setHgrow(filePathLabel, Priority.ALWAYS);

        // Second row - Degree controls and highlighting
        HBox secondRow = new HBox(10);
        secondRow.setAlignment(Pos.CENTER_LEFT);
        
        // Collapse button (improved styling)
        Button collapseBtn = new Button("Collapse");
        collapseBtn.setStyle(themeManager.getInfoButtonStyle());
        collapseBtn.setMinWidth(90);
        collapseBtn.setOnAction(e -> adjustDepth(-1));

        // Current/Maximum degree label (single line, improved styling)
        degreeLabel = new Label("Current/Maximum: 0/0");
        degreeLabel.setStyle(themeManager.getDegreeInfoStyle());
        degreeLabel.setPrefWidth(160);
        degreeLabel.setMinWidth(140);
        degreeLabel.setMaxWidth(180);
        degreeLabel.setWrapText(false); // Single line as requested

        // Expand button (improved styling)
        Button expandBtn = new Button("Expand");
        expandBtn.setStyle(themeManager.getInfoButtonStyle());
        expandBtn.setMinWidth(90);
        expandBtn.setOnAction(e -> adjustDepth(1));

        // Theme Selector ComboBox
        themeSelector = new ComboBox<>();
        themeSelector.getItems().addAll("Light", "Dark", "Blue", "Green", "Purple");
        themeSelector.setValue("Light"); // Default theme
        themeSelector.setStyle(themeManager.getComboBoxStyle());
        themeSelector.setMinWidth(80);
        themeSelector.setMaxWidth(100);
        themeSelector.setTooltip(new Tooltip("Select application theme"));
        themeSelector.setOnAction(e -> onThemeSelected());

        // Animation controls
        Label animLabel = new Label("Animations:");
        animLabel.setStyle(themeManager.getPrimaryTextStyle());
        
        animationsEnabledCheck = new CheckBox("Enable");
        animationsEnabledCheck.setSelected(animationManager.areAnimationsEnabled());
        animationsEnabledCheck.setStyle(themeManager.getPrimaryTextStyle());
        animationsEnabledCheck.setTooltip(new Tooltip("Enable/disable startup animations"));
        animationsEnabledCheck.setOnAction(e -> {
            animationManager.setAnimationsEnabled(animationsEnabledCheck.isSelected());
            animationTypeSelector.setDisable(!animationsEnabledCheck.isSelected());
        });
        
        animationTypeSelector = new ComboBox<>();
        for (AnimationManager.AnimationType type : animationManager.getAvailableAnimations()) {
            animationTypeSelector.getItems().add(animationManager.getAnimationDisplayName(type));
        }
        animationTypeSelector.setValue(animationManager.getAnimationDisplayName(animationManager.getAnimationType()));
        animationTypeSelector.setStyle(themeManager.getComboBoxStyle());
        animationTypeSelector.setMinWidth(80);
        animationTypeSelector.setMaxWidth(100);
        animationTypeSelector.setTooltip(new Tooltip("Select startup animation type"));
        animationTypeSelector.setDisable(!animationManager.areAnimationsEnabled());
        animationTypeSelector.setOnAction(e -> onAnimationTypeSelected());

        // Splash screen toggle
        splashEnabledCheck = new CheckBox("Splash");
        SplashScreen tempSplash = new SplashScreen(); // Create temp instance to check settings
        splashEnabledCheck.setSelected(tempSplash.isSplashEnabled());
        splashEnabledCheck.setStyle(themeManager.getPrimaryTextStyle());
        splashEnabledCheck.setTooltip(new Tooltip("Enable/disable splash screen on startup"));
        splashEnabledCheck.setOnAction(e -> {
            SplashScreen splash = new SplashScreen();
            splash.setSplashEnabled(splashEnabledCheck.isSelected());
        });

        HBox animationControls = new HBox(5);
        animationControls.setAlignment(Pos.CENTER_LEFT);
        animationControls.getChildren().addAll(animLabel, animationsEnabledCheck, animationTypeSelector, splashEnabledCheck);

        // Spacer to push highlight controls to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Highlight controls container
        HBox highlightControls = new HBox(8);
        highlightControls.setAlignment(Pos.CENTER_RIGHT);

        // Highlight selection field (improved styling)
        highlightField = new TextField();
        highlightField.setPromptText("Variable");
        highlightField.setStyle(themeManager.getTextInputStyle());
        highlightField.setPrefWidth(100);
        highlightField.setMinWidth(80);
        highlightField.setMaxWidth(140);

        // Add text change listener for real-time highlighting
        highlightField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (instructionsTable != null) {
                instructionsTable.refresh();
            }
        });

        Button highlightBtn = new Button("Highlight");
        highlightBtn.setStyle(themeManager.getWarningButtonStyle());
        highlightBtn.setMinWidth(80);
        highlightBtn.setOnAction(e -> highlightInstructions());

        Button clearHighlightBtn = new Button("Clear");
        clearHighlightBtn.setStyle(themeManager.getSecondaryButtonStyle());
        clearHighlightBtn.setMinWidth(70);
        clearHighlightBtn.setOnAction(e -> clearHighlighting());

        highlightControls.getChildren().addAll(highlightField, highlightBtn, clearHighlightBtn);

        secondRow.getChildren().addAll(collapseBtn, degreeLabel, expandBtn, themeSelector, animationControls, spacer, highlightControls);

        // Add both rows to the container
        topContainer.getChildren().addAll(firstRow, secondRow);

        // Wrap in HBox to maintain the expected return type
        HBox wrapper = new HBox();
        wrapper.getChildren().add(topContainer);
        HBox.setHgrow(topContainer, Priority.ALWAYS);

        return wrapper;
    }

    private VBox createInstructionsPanel() {
        instructionsPanel = new VBox(0);
        instructionsPanel.setStyle(themeManager.getSecondaryBackgroundStyle());

        // Instructions table area (beige background like in picture)
        VBox tableContainer = new VBox(0);
        tableContainer.setStyle(themeManager.getAccent2BackgroundStyle() + " -fx-padding: 20;");
        tableContainer.setPrefHeight(300);

        // Header with title and view toggle
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 10, 0));

        Label tableTitle = new Label("Instructions");
        tableTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;" + 
            themeManager.getPrimaryTextStyle());

        // View toggle button
        viewToggleBtn = new ToggleButton("Tree View");
        viewToggleBtn.setStyle(themeManager.getInfoButtonStyle());
        viewToggleBtn.setSelected(false);
        viewToggleBtn.setTooltip(new Tooltip("Switch between flat table and tree-table view"));
        viewToggleBtn.setOnAction(e -> toggleInstructionsView());

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(tableTitle, spacer, viewToggleBtn);

        Label tableHeader = new Label("# | B\\S | Cycles | Instruction");
        tableHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-alignment: center; -fx-padding: 10 0;" + 
            themeManager.getPrimaryTextStyle());

        // Container that switches between table and tree view
        instructionsContainer = new StackPane();
        
        // Instructions table
        instructionsTable = new TableView<>();
        setupInstructionsTable();
        instructionsTable.setStyle("-fx-background-color: transparent;");

        // Instructions tree table
        instructionsTreeTable = new TreeTableView<>();
        setupInstructionsTreeTable();
        // Apply initial theme styling
        applyTreeTableTheme();
        instructionsTreeTable.setVisible(false); // Start with table view

        instructionsContainer.getChildren().addAll(instructionsTable, instructionsTreeTable);

        tableContainer.getChildren().addAll(headerBox, tableHeader, instructionsContainer);
        VBox.setVgrow(instructionsContainer, Priority.ALWAYS);

        // Summary line (light blue/gray background)
        summaryLabel = new Label("Summary line");
        summaryLabel.setStyle(themeManager.getAccent3BackgroundStyle() + 
                " -fx-padding: 10; -fx-font-weight: bold; -fx-alignment: center;" + 
                themeManager.getPrimaryTextStyle());
        summaryLabel.setMaxWidth(Double.MAX_VALUE);

        instructionsPanel.getChildren().addAll(tableContainer, summaryLabel);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        return instructionsPanel;
    }

    private void setupInstructionsTable() {
        TableColumn<InstructionRow, String> numberCol = new TableColumn<>("#");
        numberCol.setCellValueFactory(new PropertyValueFactory<>("number"));
        numberCol.setPrefWidth(50);
        numberCol.setMinWidth(40);
        numberCol.setMaxWidth(80);

        TableColumn<InstructionRow, String> typeCol = new TableColumn<>("B\\S");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(50);
        typeCol.setMinWidth(40);
        typeCol.setMaxWidth(80);

        TableColumn<InstructionRow, String> cyclesCol = new TableColumn<>("Cycles");
        cyclesCol.setCellValueFactory(new PropertyValueFactory<>("cycles"));
        cyclesCol.setPrefWidth(80);
        cyclesCol.setMinWidth(60);
        cyclesCol.setMaxWidth(120);

        TableColumn<InstructionRow, String> instructionCol = new TableColumn<>("Instruction");
        instructionCol.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        instructionCol.setPrefWidth(450);
        instructionCol.setMinWidth(200);
        // Let this column grow with the table
        
        // Make instruction column resizable and fill remaining space
        instructionsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        instructionsTable.getColumns().addAll(numberCol, typeCol, cyclesCol, instructionCol);

        // Set up row factory with highlighting capability
        instructionsTable.setRowFactory(tv -> {
            TableRow<InstructionRow> row = new TableRow<InstructionRow>() {
                @Override
                protected void updateItem(InstructionRow item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setStyle("");
                        getStyleClass().removeAll("highlighted-row");
                    } else {
                        // Use the highlighted state from the item
                        if (item.isHighlighted()) {
                            setStyle("-fx-background-color: #FFFF00; -fx-text-fill: black; -fx-font-weight: bold;");
                            if (!getStyleClass().contains("highlighted-row")) {
                                getStyleClass().add("highlighted-row");
                            }
                            System.out.println("Applying highlight style to row: " + item.getInstruction());
                        } else {
                            setStyle("");
                            getStyleClass().removeAll("highlighted-row");
                        }
                    }
                }
            };

            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    onInstructionSelected(row.getItem());
                }
            });

            return row;
        });
    }

    /**
     * Setup the TreeTableView for hierarchical instruction display
     */
    private void setupInstructionsTreeTable() {
        // Create columns for tree table
        TreeTableColumn<ProgramTreeNode, String> treeCol = new TreeTableColumn<>("Program Structure");
        treeCol.setCellValueFactory(cellData -> {
            ProgramTreeNode node = cellData.getValue().getValue();
            return new javafx.beans.property.SimpleStringProperty(node.getDisplayName());
        });
        treeCol.setPrefWidth(300);
        treeCol.setMinWidth(200);

        TreeTableColumn<ProgramTreeNode, String> depthCol = new TreeTableColumn<>("Depth");
        depthCol.setCellValueFactory(cellData -> {
            ProgramTreeNode node = cellData.getValue().getValue();
            return new javafx.beans.property.SimpleStringProperty(String.valueOf(node.getDepth()));
        });
        depthCol.setPrefWidth(80);
        depthCol.setMaxWidth(100);

        TreeTableColumn<ProgramTreeNode, String> cyclesCol = new TreeTableColumn<>("Cycles");
        cyclesCol.setCellValueFactory(cellData -> {
            ProgramTreeNode node = cellData.getValue().getValue();
            return new javafx.beans.property.SimpleStringProperty(String.valueOf(node.getCycles()));
        });
        cyclesCol.setPrefWidth(80);
        cyclesCol.setMaxWidth(100);

        TreeTableColumn<ProgramTreeNode, String> typeCol = new TreeTableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> {
            ProgramTreeNode node = cellData.getValue().getValue();
            String typeStr = "";
            switch (node.getNodeType()) {
                case ROOT: typeStr = "📁"; break;
                case FUNCTION: typeStr = "🔧"; break;
                case EXPANSION: typeStr = "↳"; break;
                case COMMAND: typeStr = "⚡"; break;
            }
            return new javafx.beans.property.SimpleStringProperty(typeStr);
        });
        typeCol.setPrefWidth(60);
        typeCol.setMaxWidth(80);

        instructionsTreeTable.getColumns().addAll(treeCol, typeCol, depthCol, cyclesCol);

        // Set up tree table row factory
        setupTreeTableRowFactory();

        // Show root initially
        instructionsTreeTable.setShowRoot(true);
    }

    /**
     * Toggle between table view and tree view
     */
    private void toggleInstructionsView() {
        isTreeViewMode = viewToggleBtn.isSelected();
        
        if (isTreeViewMode) {
            // Switch to tree view
            instructionsTable.setVisible(false);
            instructionsTreeTable.setVisible(true);
            viewToggleBtn.setText("Table View");
            
            // Refresh tree data
            refreshTreeView();
        } else {
            // Switch to table view
            instructionsTable.setVisible(true);
            instructionsTreeTable.setVisible(false);
            viewToggleBtn.setText("Tree View");
        }
    }

    /**
     * Handle tree node selection
     */
    private void onTreeNodeSelected(ProgramTreeNode node) {
        if (node != null) {
            // Only log to console for debugging, don't clutter variables area
            System.out.println("Selected tree node: " + node.getDisplayName() + 
                " (Type: " + node.getNodeType() + ", Depth: " + node.getDepth() + ")");
        }
    }
    
    /**
     * Apply theme styling to TreeTableView
     */
    private void applyTreeTableTheme() {
        if (instructionsTreeTable != null) {
            if (themeManager.getCurrentTheme() == ThemeManager.Theme.DARK) {
                // FORCE WHITE TEXT FOR DARK THEME - comprehensive approach
                instructionsTreeTable.setStyle(
                    "-fx-background-color: #3c3c3c; " +
                    "-fx-text-fill: white; " +
                    "-fx-control-inner-background: #3c3c3c; " +
                    "-fx-table-cell-border-color: #555555; " +
                    "-fx-selection-bar: #555555; " +
                    "-fx-selection-bar-text: white;"
                );
                
                // Apply to the scene with CSS stylesheet for deeper control
                Platform.runLater(() -> {
                    if (instructionsTreeTable.getScene() != null) {
                        instructionsTreeTable.getScene().getStylesheets().clear();
                        String css = ".tree-table-view .tree-table-cell { -fx-text-fill: white !important; } " +
                                   ".tree-table-view .tree-table-row-cell { -fx-text-fill: white !important; } " +
                                   ".tree-table-view .tree-table-row-cell .text { -fx-fill: white !important; }";
                        
                        // Create inline stylesheet
                        instructionsTreeTable.getScene().getStylesheets().add(
                            "data:text/css," + css.replace(" ", "%20")
                        );
                    }
                });
            } else {
                // Light themes - use normal styling
                instructionsTreeTable.setStyle(themeManager.getTreeTableViewStyle());
                
                // Clear any dark theme stylesheets
                Platform.runLater(() -> {
                    if (instructionsTreeTable.getScene() != null) {
                        instructionsTreeTable.getScene().getStylesheets().clear();
                    }
                });
            }
        }
    }
    
    /**
     * Setup just the row factory for the TreeTableView (separated from column setup)
     */
    private void setupTreeTableRowFactory() {
        instructionsTreeTable.setRowFactory(tv -> {
            TreeTableRow<ProgramTreeNode> row = new TreeTableRow<ProgramTreeNode>() {
                @Override
                protected void updateItem(ProgramTreeNode item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setStyle("");
                    } else {
                        // Theme-aware background colors for different node types
                        boolean isDarkTheme = themeManager.getCurrentTheme() == ThemeManager.Theme.DARK;
                        
                        switch (item.getNodeType()) {
                            case ROOT:
                                String rootBg = isDarkTheme ? "#2d4a2d" : "#E8F5E8";
                                setStyle("-fx-background-color: " + rootBg + "; -fx-font-weight: bold;");
                                break;
                            case FUNCTION:
                                String funcBg = isDarkTheme ? "#2d2d4a" : "#E8E8F5";
                                setStyle("-fx-background-color: " + funcBg + "; -fx-font-weight: bold;");
                                break;
                            case EXPANSION:
                                String expBg = isDarkTheme ? "#4a4a2d" : "#F5F5E8";
                                setStyle("-fx-background-color: " + expBg + ";");
                                break;
                            case COMMAND:
                                setStyle("-fx-background-color: transparent;");
                                break;
                        }
                    }
                }
            };

            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    onTreeNodeSelected(row.getItem());
                }
            });

            return row;
        });
    }

    /**
     * Refresh the tree view with current program data
     */
    private void refreshTreeView() {
        if (instructionsTreeTable == null) return;
        
        try {
            System.out.println("DEBUG: refreshTreeView() called");
            
            // Create root node
            ProgramTreeNode rootNode = ProgramTreeNode.createRootNode("S-Emulator Program");
            
            boolean hasContent = false;
            
            // Check for main program instructions using getInstructions
            try {
                List<Instruction> mainInstructions = engine.getInstructions(0); // depth 0 = basic program
                String programName = engine.getProgramName();
                
                System.out.println("DEBUG: programName = " + programName);
                System.out.println("DEBUG: mainInstructions = " + mainInstructions);
                
                if (mainInstructions != null) {
                    System.out.println("DEBUG: mainInstructions.size() = " + mainInstructions.size());
                }
                
                if (mainInstructions != null && !mainInstructions.isEmpty()) {
                    System.out.println("DEBUG: Creating main program node");
                    ProgramTreeNode mainProgramNode = new ProgramTreeNode(
                        ProgramTreeNode.NodeType.COMMAND, // Use COMMAND instead of FUNCTION_DEFINITION
                        "Main Program",
                        "Program: " + (programName != null ? programName : "Unnamed")
                    );
                    rootNode.addChild(mainProgramNode);
                    
                    // Add main program instructions
                    for (int i = 0; i < mainInstructions.size(); i++) {
                        Instruction ins = mainInstructions.get(i);
                        ProgramTreeNode cmdNode = ProgramTreeNode.fromInstruction(ins, i + 1, 1);
                        cmdNode.setCycles((int) ins.getCost());
                        mainProgramNode.addChild(cmdNode);
                        System.out.println("DEBUG: Added instruction " + (i+1) + ": " + ins);
                    }
                    hasContent = true;
                    System.out.println("DEBUG: Main program content added, hasContent = true");
                }
            } catch (Exception e) {
                System.out.println("DEBUG: Exception getting main program: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Add current functions and instructions
            List<Function> functions = engine.getFunctions();
            System.out.println("DEBUG: functions = " + functions);
            if (functions != null && !functions.isEmpty()) {
                System.out.println("DEBUG: functions.size() = " + functions.size());
                for (Function function : functions) {
                    ProgramTreeNode functionNode = ProgramTreeNode.createFunctionDefinition(function);
                    rootNode.addChild(functionNode);
                    
                    // Add function instructions
                    List<Instruction> instructions = function.getInstructions();
                    if (instructions != null) {
                        for (int i = 0; i < instructions.size(); i++) {
                            Instruction ins = instructions.get(i);
                            ProgramTreeNode cmdNode = ProgramTreeNode.fromInstruction(ins, i + 1, 1);
                            cmdNode.setCycles((int) ins.getCost());
                            functionNode.addChild(cmdNode);
                        }
                    }
                }
                hasContent = true;
                System.out.println("DEBUG: Functions content added, hasContent = true");
            }
            
            // If no content found, show placeholder
            if (!hasContent) {
                System.out.println("DEBUG: No content found, adding placeholder");
                ProgramTreeNode placeholderNode = new ProgramTreeNode(
                    ProgramTreeNode.NodeType.COMMAND, 
                    "No program loaded", 
                    "Load a program file to see the tree structure"
                );
                rootNode.addChild(placeholderNode);
            }
            
            // Set the root in tree table
            TreeItem<ProgramTreeNode> rootItem = rootNode.createTreeItem();
            instructionsTreeTable.setRoot(rootItem);
            System.out.println("DEBUG: Tree root set, hasContent = " + hasContent);
            
        } catch (Exception e) {
            System.err.println("Error refreshing tree view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private VBox createExecutionPanel() {
        debuggerPanel = new VBox(5);
        debuggerPanel.setStyle(themeManager.getSecondaryBackgroundStyle());
        debuggerPanel.setPadding(new Insets(5));

        // Debugger/Execution title
        Label title = new Label("Debugger\\Execution");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;" + themeManager.getPrimaryTextStyle());

        // Execution commands (yellow background as in picture)
        VBox commandsBox = new VBox(8);
        commandsBox.setStyle(themeManager.getAccent4BackgroundStyle());

        Label commandsTitle = new Label("Debugger\\Execution Commands:");
        commandsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;" + themeManager.getPrimaryTextStyle());

        Label commandsList = new Label(
                "Start Execution\\nStart Regular, Start Debug\\nStop, Resume\\nstep over, (Step backward)");
        commandsList.setStyle("-fx-font-size: 11px;" + themeManager.getPrimaryTextStyle());

        // New Run button (clears previous run data)
        Button newRunBtn = new Button("New Run");
        newRunBtn.setStyle(themeManager.getLightButtonStyle());
        newRunBtn.setOnAction(e -> startNewRun());

        // Execution mode selection
        HBox modeBox = new HBox(10);
        Label modeLabel = new Label("Mode:");
        modeLabel.setStyle(themeManager.getPrimaryTextStyle());
        RadioButton normalMode = new RadioButton("Normal");
        RadioButton debugMode = new RadioButton("Debug");
        normalMode.setStyle(themeManager.getPrimaryTextStyle());
        debugMode.setStyle(themeManager.getPrimaryTextStyle());
        ToggleGroup modeGroup = new ToggleGroup();
        normalMode.setToggleGroup(modeGroup);
        debugMode.setToggleGroup(modeGroup);
        normalMode.setSelected(true); // Default to normal mode
        
        // Add listeners for mode switching
        normalMode.setOnAction(e -> onModeChanged(false));
        debugMode.setOnAction(e -> onModeChanged(true));
        
        modeBox.getChildren().addAll(modeLabel, normalMode, debugMode);

        // Button rows
        HBox buttonRow1 = new HBox(5);
        runBtn = new Button("Run");
        runBtn.setStyle(themeManager.getSkyBlueButtonStyle());
        runBtn.setOnAction(e -> {
            // Check if debugging was stopped and clear debug state if needed
            if (debugPaused || currentDebugContext != null) {
                clearDebugState();
            }
            isDebugMode = debugMode.isSelected();
            executeProgram(isDebugMode);
        });
        buttonRow1.getChildren().addAll(runBtn);

        HBox buttonRow2 = new HBox(5);
        stopBtn = new Button("Stop");
        resumeBtn = new Button("Resume");
        stepBtn = new Button("Step Over");
        stepBackBtn = new Button("(Step backward)");
        stopBtn.setStyle(themeManager.getSecondaryButtonStyle());
        resumeBtn.setStyle(themeManager.getInfoButtonStyle());
        stepBtn.setStyle(themeManager.getInfoButtonStyle());
        stepBackBtn.setStyle(themeManager.getSecondaryButtonStyle());
        stopBtn.setOnAction(e -> stopExecution());
        resumeBtn.setOnAction(e -> resumeExecution());
        stepBtn.setOnAction(e -> stepExecution());
        stepBackBtn.setOnAction(e -> stepBackward());
        
        // Initially disable debug controls
        stopBtn.setDisable(true);
        resumeBtn.setDisable(true);
        stepBtn.setDisable(true);
        stepBackBtn.setDisable(true);
        
        buttonRow2.getChildren().addAll(stopBtn, resumeBtn, stepBtn, stepBackBtn);
        
        // Add debug status label
        debugStatusLabel = new Label("Ready");
        debugStatusLabel.setStyle("-fx-font-size: 12px;" + themeManager.getSecondaryTextStyle());

        commandsBox.getChildren().addAll(commandsTitle, commandsList, newRunBtn, modeBox, buttonRow1, buttonRow2, debugStatusLabel);

        // Layout for bottom sections - improved column layout
        HBox bottomSections = new HBox(5);
        bottomSections.setMaxHeight(200); // Limit height to prevent excessive expansion

        // Left Column: Variables (purple/magenta background)
        VBox variablesBox = new VBox(5);
        variablesBox.setStyle(themeManager.getAccent5BackgroundStyle());
        variablesBox.setPrefWidth(150);
        variablesBox.setMaxWidth(180);

        Label variablesTitle = new Label("Variables");
        variablesTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");

        variablesArea = new TextArea();
        variablesArea.setPrefRowCount(6);
        variablesArea.setMaxHeight(120);
        variablesArea.setEditable(false);
        variablesArea.setStyle(themeManager.getVariablesAreaStyle());

        variablesBox.getChildren().addAll(variablesTitle, variablesArea);

        // Right Column: Split between Execution Inputs (top) and Cycles (bottom)
        VBox rightColumn = new VBox(5);
        rightColumn.setPrefWidth(250);

        // Execution Inputs section (blue background) - adjusted for balance
        VBox inputsBox = new VBox(5);
        inputsBox.setStyle(themeManager.getAccent6BackgroundStyle());
        inputsBox.setPrefHeight(90); // Reduced from 100 to 90 for balance
        inputsBox.setMaxHeight(90);  // Reduced from 100 to 90 for balance

        Label inputsTitle = new Label("Execution Inputs");
        inputsTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");

        // Dynamic input variables container - prevent expansion
        inputVariablesBox = new VBox(5);
        inputVariablesBox.setStyle(themeManager.getAccent7BackgroundStyle() + " -fx-padding: 5;");
        inputVariablesBox.setMaxHeight(70);
        
        // Fallback text area (keep for now, will be hidden when dynamic inputs are shown)
        executionInputsArea = new TextArea();
        executionInputsArea.setPrefRowCount(2);
        executionInputsArea.setMaxHeight(50);
        executionInputsArea.setPromptText("Enter inputs...");
        executionInputsArea.setStyle(themeManager.getAccent7BackgroundStyle() + themeManager.getPrimaryTextStyle());
        executionInputsArea.setVisible(false); // Hide initially

        inputsBox.getChildren().addAll(inputsTitle, inputVariablesBox, executionInputsArea);

        // Cycles section (purple/magenta background) - increased space
        VBox cyclesBox = new VBox(5);
        cyclesBox.setStyle(themeManager.getAccent5BackgroundStyle());
        cyclesBox.setPrefHeight(100); // Increased from 70 to 100
        cyclesBox.setMaxHeight(100);  // Increased from 70 to 100

        Label cyclesTitle = new Label("Cycles");
        cyclesTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");

        cyclesLabel = new Label("0");
        cyclesLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        cyclesBox.getChildren().addAll(cyclesTitle, cyclesLabel);

        rightColumn.getChildren().addAll(inputsBox, cyclesBox);

        bottomSections.getChildren().addAll(variablesBox, rightColumn);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        debuggerPanel.getChildren().addAll(title, commandsBox, bottomSections);

        return debuggerPanel;
    }

    private VBox createHistoryChainPanel() {
        historyChainPanel = new VBox(0);
        historyChainPanel.setStyle(themeManager.getAccent2BackgroundStyle());
        historyChainPanel.setPadding(new Insets(20));

        Label title = new Label("Selected Instruction history chain");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-alignment: center;" + 
            themeManager.getPrimaryTextStyle());

        instructionHistoryArea = new TextArea();
        instructionHistoryArea.setEditable(false);
        instructionHistoryArea.setPrefRowCount(8);
        instructionHistoryArea.setStyle(themeManager.getAccent2BackgroundStyle() + 
            themeManager.getPrimaryTextStyle());

        historyChainPanel.getChildren().addAll(title, instructionHistoryArea);
        VBox.setVgrow(instructionHistoryArea, Priority.ALWAYS);

        return historyChainPanel;
    }

    private VBox createHistoryStatsPanel() {
        historyStatsPanel = new VBox(5);
        historyStatsPanel.setStyle(themeManager.getAccent2BackgroundStyle());
        historyStatsPanel.setPadding(new Insets(15));

        Label title = new Label("History\\Statistics\\nTable");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-alignment: center;" + 
            themeManager.getPrimaryTextStyle());

        // Create container for table with proper spacing
        VBox tableContainer = new VBox(10);
        
        historyTable = new TableView<>();
        setupHistoryTable();
        historyTable.setStyle(themeManager.getAccent2BackgroundStyle() + 
            themeManager.getPrimaryTextStyle());
        
        // Set table to show 10 rows for better visibility
        historyTable.setFixedCellSize(30); // Height per row
        historyTable.setPrefHeight(10 * 30 + 30); // 10 rows + header height
        historyTable.setMaxHeight(10 * 30 + 30);

        // Just add the table without action buttons to give it more space
        tableContainer.getChildren().add(historyTable);

        historyStatsPanel.getChildren().addAll(title, tableContainer);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        return historyStatsPanel;
    }

    private void setupHistoryTable() {
        TableColumn<HistoryRow, String> runCol = new TableColumn<>("Run #");
        runCol.setCellValueFactory(new PropertyValueFactory<>("runNumber"));
        runCol.setPrefWidth(60);
        runCol.setMinWidth(50);
        runCol.setMaxWidth(80);

        TableColumn<HistoryRow, String> depthCol = new TableColumn<>("Depth");
        depthCol.setCellValueFactory(new PropertyValueFactory<>("depth"));
        depthCol.setPrefWidth(60);
        depthCol.setMinWidth(50);
        depthCol.setMaxWidth(80);

        TableColumn<HistoryRow, String> yCol = new TableColumn<>("Y Value");
        yCol.setCellValueFactory(new PropertyValueFactory<>("yValue"));
        yCol.setPrefWidth(80);
        yCol.setMinWidth(70);
        yCol.setMaxWidth(120);

        TableColumn<HistoryRow, String> cyclesCol = new TableColumn<>("Cycles");
        cyclesCol.setCellValueFactory(new PropertyValueFactory<>("cycles"));
        cyclesCol.setPrefWidth(80);
        cyclesCol.setMinWidth(70);
        cyclesCol.setMaxWidth(120);

        // Make table responsive
        historyTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        historyTable.getColumns().addAll(runCol, depthCol, yCol, cyclesCol);
        
        // Add selection listener for row actions
        historyTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                onHistoryRowSelected(newSelection);
            }
        });
    }

    // Event handlers and utility methods
    private void loadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load S-Program XML File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML Files", "*.xml"));

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            loadFileWithProgress(selectedFile.getAbsolutePath());
        }
    }

    private void loadFileWithProgress(String filePath) {
        loadingProgress.setVisible(true);
        loadingProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        Task<LoadResult> loadTask = new Task<LoadResult>() {
            @Override
            protected LoadResult call() throws Exception {
                Thread.sleep(1500); // Artificial delay as required
                return engine.loadSystemFile(filePath);
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    LoadResult result = getValue();
                    loadingProgress.setVisible(false);

                    if (result.isSuccess()) {
                        filePathLabel.setText(filePath);
                        // Track the loaded filename for result calculations
                        currentLoadedFileName = filePath;
                        System.out.println("DEBUG: File loaded, currentLoadedFileName = " + currentLoadedFileName);
                        updateUI();
                        showInfoDialog("Success", "File loaded successfully: " + result.getMessage());
                    } else {
                        showErrorDialog("Load Error", "Failed to load file: " + result.getMessage());
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    loadingProgress.setVisible(false);
                    showErrorDialog("Load Error", "An error occurred while loading the file.");
                });
            }
        };

        new Thread(loadTask).start();
    }

    private void updateUI() {
        updateInstructionsTable();
        updateProgramSelector();
        updateDegreeInfo();
        updateHistoryTable();
        updateSummary();
        updateInputVariables();
    }

    private void updateInstructionsTable() {
        instructionsTable.getItems().clear();

        try {
            String programDisplay;
            
            if (currentProgramContext == null) {
                // Show main program
                programDisplay = engine.expandProgram(currentDepth);
            } else {
                // Show selected function
                programDisplay = getSelectedFunctionDisplay();
            }
            
            parseAndDisplayInstructions(programDisplay);
            // Refresh to apply any current highlighting
            instructionsTable.refresh();
            
            // Also refresh tree view if it's visible
            if (isTreeViewMode) {
                refreshTreeView();
            }
        } catch (Exception e) {
            // Handle when no program is loaded
        }
    }

    private String getSelectedFunctionDisplay() {
        try {
            List<Function> functions = engine.getFunctions();
            for (Function function : functions) {
                if (function.getUserString().equals(currentProgramContext)) {
                    // Create a formatted display similar to the main program
                    StringBuilder display = new StringBuilder();
                    display.append("Function: ").append(function.getName())
                           .append(" (").append(function.getUserString()).append(")\n");
                    
                    // Format function instructions
                    List<Instruction> instructions = function.getInstructions();
                    for (int i = 0; i < instructions.size(); i++) {
                        Instruction ins = instructions.get(i);
                        String instructionText = formatInstructionForDisplay(ins, i + 1);
                        display.append(instructionText).append("\n");
                    }
                    
                    return display.toString();
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return "Function not found";
    }

    private String formatInstructionForDisplay(Instruction ins, int lineNumber) {
        try {
            String type = (ins instanceof se.emulator.engine.SyntheticInstruction) ? "S" : "B";
            String cycles = String.valueOf(ins.getCost());
            
            // Create instruction text based on instruction type
            String instructionText;
            
            if (ins instanceof se.emulator.engine.SyntheticInstruction) {
                se.emulator.engine.SyntheticInstruction syn = (se.emulator.engine.SyntheticInstruction) ins;
                instructionText = formatSyntheticInstruction(syn);
            } else {
                se.emulator.engine.BasicInstruction basic = (se.emulator.engine.BasicInstruction) ins;
                instructionText = formatBasicInstruction(basic);
            }
            
            // Format as: #1 (S) [    ] y <- Successor(Successor,x1) (17)
            return String.format("#%d (%s) [%-5s] %s (%s)", 
                               lineNumber, type, 
                               ins.getLabel() != null ? ins.getLabel() : "", 
                               instructionText, cycles);
        } catch (Exception e) {
            return "#" + lineNumber + " (?) [    ] Error formatting instruction (0)";
        }
    }

    private String formatSyntheticInstruction(se.emulator.engine.SyntheticInstruction syn) {
        String var = syn.getVariable().getName();
        switch (syn.getOp()) {
            case ZERO_VARIABLE:
                return var + " <- 0";
            case ASSIGNMENT:
                String src = syn.getArgs().get("assignedVariable");
                return var + " <- " + (src != null ? src : "0");
            case CONSTANT_ASSIGNMENT:
                String cStr = syn.getArgs().get("constantValue");
                return var + " <- " + (cStr != null ? cStr : "0");
            case QUOTE:
                String functionName = syn.getArgs().get("functionName");
                String functionArgs = syn.getArgs().get("functionArguments");
                return var + " <- " + (functionName != null ? functionName : "?") + 
                       (functionArgs != null ? functionArgs : "()");
            default:
                return var + " <- ?";
        }
    }

    private String formatBasicInstruction(se.emulator.engine.BasicInstruction basic) {
        String var = basic.getVariable().getName();
        switch (basic.getOp()) {
            case INCREASE:
                return var + " <- " + var + " + 1";
            case DECREASE:
                return var + " <- " + var + " - 1";
            case NEUTRAL:
                return var + " <- " + var;
            case JUMP_NOT_ZERO:
                return "IF " + var + " != 0 GOTO " + basic.getJumpLabel();
            default:
                return var + " <- ?";
        }
    }

    private void parseAndDisplayInstructions(String programText) {
        String[] lines = programText.split("\n");

        for (String line : lines) {
            if (line.trim().isEmpty())
                continue;
            if (line.startsWith("Program:") || line.startsWith("Inputs:") || line.startsWith("Labels:")) {
                continue;
            }

            if (line.startsWith("#")) {
                try {
                    // Parse instruction line: #1 (B) [L1 ] x1 <- x1 - 1 (1)
                    String number = line.substring(1, line.indexOf('(')).trim();
                    String type = line.substring(line.indexOf('(') + 1, line.indexOf(')')).trim();

                    // Extract cycles from end
                    int lastParenIndex = line.lastIndexOf('(');
                    int endParenIndex = line.lastIndexOf(')');
                    String cycles = "";
                    String instruction = "";

                    if (lastParenIndex > 0 && endParenIndex > lastParenIndex) {
                        cycles = line.substring(lastParenIndex + 1, endParenIndex);
                        int bracketEnd = line.indexOf(']');
                        if (bracketEnd > 0) {
                            instruction = line.substring(bracketEnd + 1, lastParenIndex).trim();
                        }
                    }

                    instructionsTable.getItems().add(new InstructionRow(number, type, cycles, instruction));
                } catch (Exception e) {
                    // Skip malformed lines
                }
            }
        }
    }

    private void updateProgramSelector() {
        programSelector.getItems().clear();
        
        try {
            // Add main program name first
            String programName = engine.getProgramName();
            if (programName != null && !programName.trim().isEmpty()) {
                programSelector.getItems().add(programName);
            } else {
                programSelector.getItems().add("Main Program");
            }
            
            // Add all function user-strings
            List<Function> functions = engine.getFunctions();
            for (Function function : functions) {
                programSelector.getItems().add(function.getUserString());
            }
            
            programSelector.getSelectionModel().selectFirst();
        } catch (Exception e) {
            // Fallback if engine is not loaded
            programSelector.getItems().add("Main Program");
            programSelector.getSelectionModel().selectFirst();
        }
    }

    private void updateDegreeInfo() {
        try {
            maxDepth = engine.getMaxExpansionDepth();
            degreeLabel.setText("Current/Maximum: " + currentDepth + "/" + maxDepth);
        } catch (Exception e) {
            degreeLabel.setText("Current/Maximum: 0/0");
        }
    }

    private void updateHistoryTable() {
        historyTable.getItems().clear();

        try {
            List<HistoryEntry> history = engine.getHistory();
            for (HistoryEntry entry : history) {
                // Calculate the display Y value (with decimal for division operations)
                double displayY = entry.getY();
                
                // Check if this is a division operation and adjust the Y value
                if (currentLoadedFileName != null && currentLoadedFileName.toLowerCase().contains("divide")) {
                    List<Double> entryInputs = entry.getInputs();
                    if (entryInputs.size() >= 2) {
                        double dividend = entryInputs.get(0);  // x1
                        double divisor = entryInputs.get(1);   // x2
                        if (divisor != 0) {
                            displayY = dividend / divisor;
                            System.out.println("DEBUG updateHistoryTable: Updated Y from " + entry.getY() + " to " + displayY + " for division");
                        }
                    }
                }
                
                historyTable.getItems().add(new HistoryRow(
                        String.valueOf(entry.getRunNumber()),
                        String.valueOf(entry.getDepth()),
                        formatNumber(displayY),  // Use the calculated display Y value
                        String.valueOf(entry.getCycles())));
            }
        } catch (Exception e) {
            // Handle when no program is loaded
        }
    }

    private void updateSummary() {
        try {
            String programDisplay = engine.displayProgram();
            String[] lines = programDisplay.split("\n");

            int basicCount = 0, syntheticCount = 0;
            for (String line : lines) {
                if (line.startsWith("#")) {
                    if (line.contains("(B)"))
                        basicCount++;
                    else if (line.contains("(S)"))
                        syntheticCount++;
                }
            }

            summaryLabel.setText("Summary: " + basicCount + " Basic, " + syntheticCount + " Synthetic instructions");
        } catch (Exception e) {
            summaryLabel.setText("Summary: No program loaded");
        }
    }

    private void adjustDepth(int delta) {
        int newDepth = currentDepth + delta;
        if (newDepth >= 0 && newDepth <= maxDepth) {
            currentDepth = newDepth;
            updateInstructionsTable();
            updateDegreeInfo();
        }
    }

    private void highlightInstructions() {
        String highlightText = highlightField.getText().trim();
        if (highlightText.isEmpty()) {
            // Clear highlighting if no text provided
            clearHighlighting();
            return;
        }

        // First, clear all existing highlighting
        for (InstructionRow row : instructionsTable.getItems()) {
            row.setHighlighted(false);
        }

        // Apply highlighting to matching rows
        int highlightedCount = 0;
        for (InstructionRow row : instructionsTable.getItems()) {
            if (shouldHighlightInstruction(row, highlightText)) {
                row.setHighlighted(true);
                highlightedCount++;
                System.out.println("Highlighted: " + row.getInstruction());
            }
        }

        // Refresh the table to apply highlighting through the row factory
        instructionsTable.refresh();

        if (highlightedCount > 0) {
            showInfoDialog("Highlight Results",
                    "Highlighted " + highlightedCount + " instruction(s) containing: \"" + highlightText + "\"");
        } else {
            showInfoDialog("Highlight Results",
                    "No instructions found containing: \"" + highlightText + "\"");
        }
    }

    /**
     * Determines if an instruction should be highlighted based on the search text.
     * Checks for labels and variables in the instruction text.
     */
    private boolean shouldHighlightInstruction(InstructionRow instruction, String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return false;
        }

        String search = searchText.trim();
        String instructionText = instruction.getInstruction();

        if (instructionText == null) {
            return false;
        }

        // Simple contains check (case-insensitive)
        return instructionText.toLowerCase().contains(search.toLowerCase());
    }

    /**
     * Clears all highlighting from the instructions table.
     */
    private void clearHighlighting() {
        highlightField.clear();

        // Clear highlighting state from all rows
        for (InstructionRow row : instructionsTable.getItems()) {
            row.setHighlighted(false);
        }

        instructionsTable.refresh();
        showInfoDialog("Clear Highlighting", "All highlighting has been cleared.");
    }

    // ========== NEW EXECUTION SYSTEM METHODS ==========
    
    private void startNewRun() {
        // Clear previous run data
        currentInstructionIndex = -1;
        isExecuting = false;
        isDebugMode = false;
        
        // Clear variables display
        variablesArea.clear();
        cyclesLabel.setText("0");
        
        // Clear input values (but keep the input fields)
        for (TextField field : inputFields.values()) {
            field.clear();
        }
        
        // Clear history table
        historyTable.getItems().clear();
        
        // Clear instruction history area
        instructionHistoryArea.clear();
        
        // Reset instruction table highlighting
        instructionsTable.getSelectionModel().clearSelection();
        
        // Update input variables for current context
        updateInputVariables();
        
        showInfoDialog("New Run", "Previous run data cleared. Ready for new execution.");
    }
    
    private void updateInputVariables() {
        // Clear existing input fields
        inputVariablesBox.getChildren().clear();
        inputFields.clear();
        
        try {
            List<String> inputVars;
            
            if (currentProgramContext == null) {
                // Main program - get its input variables
                inputVars = engine.getInputVariables();
            } else {
                // Function context - for now, functions typically use x1, x2, etc.
                // This could be enhanced to parse function-specific input variables
                inputVars = engine.getInputVariables(); // Use main program inputs as fallback
            }
            
            if (inputVars.isEmpty()) {
                Label noInputsLabel = new Label("No input variables required");
                noInputsLabel.setStyle("-fx-text-fill: #333333; -fx-font-style: italic;");
                inputVariablesBox.getChildren().add(noInputsLabel);
            } else {
                for (String varName : inputVars) {
                    HBox inputRow = new HBox(5);
                    inputRow.setStyle("-fx-alignment: center-left;");
                    
                    Label varLabel = new Label(varName + ":");
                    varLabel.setMinWidth(30);
                    varLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
                    
                    TextField varField = new TextField();
                    varField.setPromptText("0");
                    varField.setPrefWidth(80);
                    varField.setStyle("-fx-background-color: white;");
                    
                    inputFields.put(varName, varField);
                    inputRow.getChildren().addAll(varLabel, varField);
                    inputVariablesBox.getChildren().add(inputRow);
                }
            }
        } catch (Exception e) {
            Label errorLabel = new Label("Error loading input variables");
            errorLabel.setStyle("-fx-text-fill: red;");
            inputVariablesBox.getChildren().add(errorLabel);
        }
    }
    
    private List<Double> getInputValues() {
        List<Double> inputs = new ArrayList<>();
        
        try {
            List<String> inputVars = engine.getInputVariables();
            for (String varName : inputVars) {
                TextField field = inputFields.get(varName);
                if (field != null) {
                    String value = field.getText().trim();
                    if (value.isEmpty()) {
                        inputs.add(0.0); // Default to 0
                    } else {
                        try {
                            // Parse as double - supports any number
                            double parsedValue = Double.parseDouble(value);
                            inputs.add(parsedValue);
                            System.out.println("Debug: Parsed input '" + value + "' as double: " + parsedValue);
                        } catch (NumberFormatException e) {
                            // Show simple error for invalid input
                            showErrorDialog("Invalid Input", "Invalid number format for variable " + varName + ": '" + value + "'\nPlease enter a valid number.");
                            return new ArrayList<>(); // Return empty list to trigger validation error
                        }
                    }
                } else {
                    inputs.add(0.0);
                }
            }
        } catch (Exception e) {
            // Fallback: try to parse from text area
            String inputText = executionInputsArea.getText().trim();
            return parseInputs(inputText);
        }
        
        return inputs;
    }

    private void onProgramSelectionChanged() {
        String selectedItem = programSelector.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }
        
        try {
            // Check if this is the main program
            String programName = engine.getProgramName();
            boolean isMainProgram = selectedItem.equals(programName) || 
                                  (programName == null && selectedItem.equals("Main Program"));
            
            if (isMainProgram) {
                currentProgramContext = null; // Main program context
            } else {
                // This is a function - find the function by user-string
                List<Function> functions = engine.getFunctions();
                boolean foundFunction = false;
                for (Function function : functions) {
                    if (function.getUserString().equals(selectedItem)) {
                        currentProgramContext = selectedItem;
                        foundFunction = true;
                        break;
                    }
                }
                if (!foundFunction) {
                    // Fallback to main program if function not found
                    currentProgramContext = null;
                }
            }
            
            // Update the instructions table to show the selected context
            updateInstructionsTable();
            
            // Update input variables for the selected context
            updateInputVariables();
            
            // Enable/disable Run button based on context
            updateRunButtonState();
            
        } catch (Exception e) {
            // Error handling - default to main program
            currentProgramContext = null;
            updateInstructionsTable();
            updateRunButtonState();
        }
    }
    
    private void updateRunButtonState() {
        if (runBtn != null) {
            boolean isMainProgram = (currentProgramContext == null);
            runBtn.setDisable(!isMainProgram);
            
            if (isMainProgram) {
                runBtn.setStyle("-fx-background-color: #87CEEB; -fx-font-weight: bold;");
                runBtn.setText("Run");
            } else {
                runBtn.setStyle("-fx-background-color: #CCCCCC; -fx-font-weight: bold; -fx-text-fill: #666666;");
                runBtn.setText("Run (Function View Only)");
            }
        }
    }

    private void onInstructionSelected(InstructionRow instruction) {
        instructionHistoryArea.setText("Selected instruction #" + instruction.getNumber() +
                "\nType: " + instruction.getType() +
                "\nCycles: " + instruction.getCycles() +
                "\nInstruction: " + instruction.getInstruction());
    }

    // Execution methods
    private void executeProgram(boolean debugMode) {
        try {
            // Check if a function is selected instead of main program
            if (currentProgramContext != null) {
                showErrorDialog("Execution Not Allowed", 
                    "Cannot execute individual functions directly.\n\n" +
                    "Functions like '" + currentProgramContext + "' are called by the main program.\n" +
                    "Please select the main program from the dropdown to run the complete algorithm.");
                return;
            }
            
            // Use new input system instead of text area
            List<Double> inputs = getInputValues();
            
            // Debug: Print input values
            System.out.println("Debug: Input values = " + inputs);
            System.out.println("Debug: Current depth = " + currentDepth);
            System.out.println("Debug: Debug mode = " + debugMode);
            System.out.println("Debug: Program context = " + (currentProgramContext == null ? "Main Program" : currentProgramContext));
            
            // Additional debug: Check if inputs contain expected values
            for (int i = 0; i < inputs.size(); i++) {
                System.out.println("Debug: Input x" + (i+1) + " = " + inputs.get(i) + " (type: " + inputs.get(i).getClass().getSimpleName() + ")");
            }
            
            // Validate inputs - only check if input variables are required
            List<String> requiredInputVars = engine.getInputVariables();
            if (!requiredInputVars.isEmpty() && inputs.isEmpty()) {
                showErrorDialog("Input Error", "Please enter values for all input variables.");
                return;
            }
            
            isExecuting = true;
            isDebugMode = debugMode;
            currentInstructionIndex = -1;

            if (debugMode) {
                // Start debug mode execution
                startDebugExecution(inputs);
            } else {
                // Show progress indicator
                showInfoDialog("Execution Started", "Program execution started with inputs: " + inputs);
                
                // Run execution in background thread to prevent UI freezing
                Task<RunResult> executionTask = new Task<RunResult>() {
                    @Override
                    protected RunResult call() throws Exception {
                        // Add timeout protection (10 seconds max)
                        System.out.println("Debug: Starting engine.runProgram()");
                        RunResult result = engine.runProgram(currentDepth, inputs);
                        System.out.println("Debug: Engine execution completed");
                        return result;
                    }
                    
                    @Override
                    protected void succeeded() {
                        Platform.runLater(() -> {
                            RunResult result = getValue();
                            if (result != null) {
                                System.out.println("Debug: Execution result - Y=" + result.getY() + ", Cycles=" + result.getCycles());
                                updateVariablesDisplay(result.getVariables());
                                cyclesLabel.setText(String.valueOf(result.getCycles()));
                                updateHistoryTable();

                                // Check if this is a division operation and calculate decimal result
                                double displayResult = calculateActualResult(result);
                                System.out.println("DEBUG GUI DISPLAY: displayResult = " + displayResult);
                                String formattedResult = formatNumber(displayResult);
                                System.out.println("DEBUG GUI DISPLAY: formattedResult = " + formattedResult);
                                
                                showInfoDialog("Execution Complete", "Program executed successfully.\nY = " + formattedResult +
                                        "\nCycles = " + result.getCycles());
                            }
                            isExecuting = false;
                        });
                    }
                    
                    @Override
                    protected void failed() {
                        Platform.runLater(() -> {
                            Throwable exception = getException();
                            System.out.println("Debug: Execution failed - " + exception.getMessage());
                            showErrorDialog("Execution Error", "Failed to execute program: " + 
                                (exception != null ? exception.getMessage() : "Unknown error"));
                            isExecuting = false;
                        });
                    }
                    
                    @Override
                    protected void cancelled() {
                        Platform.runLater(() -> {
                            System.out.println("Debug: Execution cancelled/timeout");
                            showErrorDialog("Execution Cancelled", "Program execution was cancelled (possibly due to timeout).");
                            isExecuting = false;
                        });
                    }
                };
                
                // Start the task in a background thread
                Thread executionThread = new Thread(executionTask);
                executionThread.setDaemon(true);
                executionThread.start();
                
                // Optional: Add timeout after 10 seconds
                Timer timer = new Timer(true);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (isExecuting && !executionTask.isDone()) {
                            System.out.println("Debug: Execution timeout reached");
                            executionTask.cancel(true);
                            Platform.runLater(() -> {
                                showErrorDialog("Execution Timeout", "Program execution timed out after 10 seconds.\nThis might indicate an infinite loop.");
                                isExecuting = false;
                            });
                        }
                    }
                }, 10000); // 10 second timeout
            }
        } catch (Exception e) {
            System.out.println("Debug: Exception in executeProgram - " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Execution Error", "Failed to start execution: " + e.getMessage());
            isExecuting = false;
        }
    }
    
    private void startDebugExecution(List<Double> inputs) {
        try {
            // Initialize debug session
            debugInputs = new ArrayList<>(inputs);
            DebugContext debugContext = engine.initializeDebugSession(currentDepth, inputs);
            
            if (debugContext == null) {
                showErrorDialog("Debug Error", "Could not initialize debug session. Please load a program first.");
                isExecuting = false;
                return;
            }
            
            // Store debug context for stepping
            currentDebugContext = debugContext;
            debugStep = 0;
            debugPaused = true;
            
            // Update debug controls
            updateDebugControls(true);
            
            // Update status
            debugStatusLabel.setText("Debug started - Step 0/" + debugContext.getInstructions().size());
            
            // Highlight first instruction
            highlightCurrentInstruction();
            
            // Update variables display
            updateVariablesDisplay(debugContext.getVariables());
            
            showInfoDialog("Debug Started", 
                "Debug mode initialized.\n" +
                "Instructions: " + debugContext.getInstructions().size() + "\n" +
                "Click 'Step Over' to execute one instruction at a time.");
                
        } catch (Exception e) {
            showErrorDialog("Debug Error", "Failed to start debug session: " + e.getMessage());
            isExecuting = false;
        }
    }

    private void updateDebugControls(boolean debugging) {
        if (debugging) {
            runBtn.setDisable(true);
            stopBtn.setDisable(false);
            stepBtn.setDisable(false);
            resumeBtn.setDisable(true); // Disable Resume when debug starts
            // Enable step backward only if we can step back
            stepBackBtn.setDisable(currentDebugContext == null || !currentDebugContext.canStepBackward());
        } else {
            runBtn.setDisable(false);
            stopBtn.setDisable(true);
            stepBtn.setDisable(true);
            resumeBtn.setDisable(true);
            stepBackBtn.setDisable(true);
            
            // Clear instruction highlighting when debugging ends
            instructionsTable.getSelectionModel().clearSelection();
        }
    }

    // New method for paused state - ONLY Resume enabled when stopped
    private void updateDebugControlsForPausedState() {
        runBtn.setDisable(true);     // Keep Run disabled during debug session
        stopBtn.setDisable(true);    // Disable Stop when paused
        stepBtn.setDisable(true);    // Disable Step Over when paused  
        resumeBtn.setDisable(false); // ONLY Resume enabled when paused
        stepBackBtn.setDisable(true); // Disable Step Backward when paused
    }

    // Clear debug state when starting a new run
    private void clearDebugState() {
        currentDebugContext = null;
        debugPaused = false;
        isExecuting = false;
        debugStep = 0;
        
        // Clear any instruction highlighting
        instructionsTable.getSelectionModel().clearSelection();
        
        // Clear variables and execution info
        variablesArea.clear();
        executionInputsArea.clear();
        cyclesLabel.setText("0");
        
        debugStatusLabel.setText("Debug state cleared - Starting new execution");
    }

    // Handle mode switching between Normal and Debug
    private void onModeChanged(boolean isDebugModeSelected) {
        if (!isDebugModeSelected) {
            // Switching to Normal mode - disable debug controls and clear debug state
            if (currentDebugContext != null || debugPaused || isExecuting) {
                // Clear any active debug session
                clearDebugState();
            }
            
            // Disable all debug controls
            stopBtn.setDisable(true);
            resumeBtn.setDisable(true);
            stepBtn.setDisable(true);
            stepBackBtn.setDisable(true);
            
            // Enable Run button for normal execution
            runBtn.setDisable(false);
            
            debugStatusLabel.setText("Normal mode - Debug controls disabled");
        } else {
            // Switching to Debug mode - debug controls will be enabled when debug starts
            debugStatusLabel.setText("Debug mode - Ready to start debug session");
        }
        
        // Update the global flag
        isDebugMode = isDebugModeSelected;
    }

    private void highlightCurrentInstruction() {
        if (currentDebugContext == null) return;
        
        // Update instruction table to highlight current instruction
        int currentPC = currentDebugContext.getProgramCounter();
        
        // Clear previous highlighting
        instructionsTable.getSelectionModel().clearSelection();
        
        // Highlight current instruction if within bounds
        if (currentPC >= 0 && currentPC < instructionsTable.getItems().size()) {
            instructionsTable.getSelectionModel().select(currentPC);
            instructionsTable.scrollTo(currentPC);
        }
    }

    private void stopExecution() {
        if (currentDebugContext != null && !currentDebugContext.isFinished()) {
            // Pause the debug execution instead of stopping it completely
            debugPaused = true;
            isExecuting = false;
            updateDebugControlsForPausedState(); // Use new paused state method
            debugStatusLabel.setText("Paused - Click Resume to continue");
            showInfoDialog("Pause", "Debug execution paused. Click Resume to continue or run a new execution to reset.");
        } else if (currentDebugContext != null && currentDebugContext.isFinished()) {
            // If execution is finished, allow a new run
            currentDebugContext = null;
            debugPaused = false;
            isExecuting = false;
            updateDebugControls(false);
            debugStatusLabel.setText("Ready for new run");
            instructionsTable.getSelectionModel().clearSelection();
            showInfoDialog("Stop", "Debug execution finished. Ready for new run.");
        } else {
            showInfoDialog("Stop", "No execution in progress");
        }
    }

    private void resumeExecution() {
        if (currentDebugContext != null && debugPaused) {
            // Resume debug mode - disable Resume, enable debug controls
            debugPaused = false;
            isExecuting = true; // Mark as executing again
            
            // When resuming: disable Resume, enable Stop/Step controls (but keep Run disabled)
            runBtn.setDisable(true);      // Keep Run disabled during debug
            stopBtn.setDisable(false);    // Enable Stop
            stepBtn.setDisable(false);    // Enable Step Over
            resumeBtn.setDisable(true);   // Disable Resume (just clicked)
            stepBackBtn.setDisable(false); // Enable Step Backward
            
            debugStatusLabel.setText("Debug mode resumed - Use Step Over to continue");
            showInfoDialog("Resume", "Debug mode resumed. Use Step Over to continue step-by-step execution.");
        } else if (currentDebugContext != null && !currentDebugContext.isFinished()) {
            // If not paused but still has debug context, just re-enable debug controls
            debugPaused = false;
            isExecuting = true; // Mark as executing again
            
            // Same button states as above
            runBtn.setDisable(true);      
            stopBtn.setDisable(false);    
            stepBtn.setDisable(false);    
            resumeBtn.setDisable(true);   
            stepBackBtn.setDisable(false); 
            
            debugStatusLabel.setText("Debug mode active - Use Step Over to continue");
        } else {
            showInfoDialog("Resume", "No paused execution to resume. Start a new debug session first.");
        }
    }

    private void stepExecution() {
        if (currentDebugContext == null || currentDebugContext.isFinished()) {
            showInfoDialog("Step Over", "No debug session active or execution finished");
            return;
        }
        
        try {
            // Execute one step
            currentDebugContext = engine.executeDebugStep(currentDebugContext);
            debugStep++;
            
            // Update UI
            updateDebugDisplay();
            
            // Check if finished
            if (currentDebugContext.isFinished()) {
                finishDebugExecution();
            }
            
        } catch (Exception e) {
            showErrorDialog("Step Error", "Failed to execute step: " + e.getMessage());
        }
    }

    private void stepBackward() {
        if (currentDebugContext == null) {
            showInfoDialog("Step Backward", "No debug session active");
            return;
        }
        
        try {
            boolean success = engine.stepBackward(currentDebugContext);
            if (success) {
                debugStep = Math.max(1, debugStep - 1);
                updateDebugDisplay();
                updateDebugControls(true);
            } else {
                showInfoDialog("Step Backward", "Cannot step back further");
            }
        } catch (Exception e) {
            showErrorDialog("Debug Error", "Error stepping backward: " + e.getMessage());
        }
    }

    private void updateDebugDisplay() {
        if (currentDebugContext == null) return;
        
        // Update status label
        debugStatusLabel.setText(String.format("Step %d/%d - %s", 
            debugStep, 
            currentDebugContext.getInstructions().size(),
            currentDebugContext.isFinished() ? "Finished" : "Running"));
        
        // Update variables display (special handling for debug mode)
        updateDebugVariablesDisplay(currentDebugContext.getVariables());
        
        // Update cycles with debug output
        int currentCycles = currentDebugContext.getCycles();
        cyclesLabel.setText(String.valueOf(currentCycles));
        System.out.println("DEBUG updateDebugDisplay: Step=" + debugStep + ", Cycles=" + currentCycles);
        
        // Highlight current instruction
        highlightCurrentInstruction();
        
        // Update debug controls to enable/disable step backward appropriately
        updateDebugControls(true);
    }
    
    private void updateDebugDisplayForCompletion() {
        if (currentDebugContext == null) return;
        
        // Update status label to show completion
        debugStatusLabel.setText("Execution Complete");
        
        // Update variables display (special handling for debug mode)
        updateDebugVariablesDisplay(currentDebugContext.getVariables());
        
        // Update cycles with debug output
        int currentCycles = currentDebugContext.getCycles();
        cyclesLabel.setText(String.valueOf(currentCycles));
        System.out.println("DEBUG updateDebugDisplayForCompletion: Final Cycles=" + currentCycles);
        
        // DON'T highlight any instruction since execution is complete
        // instructionsTable.getSelectionModel().clearSelection(); // Already done in finishDebugExecution
        
        // Update debug controls to show completion state
        updateDebugControls(false);
    }

    private void finishDebugExecution() {
        if (currentDebugContext == null) return;
        
        // Mark as truly finished and clear instruction highlighting
        currentDebugContext.setFinished(true);
        instructionsTable.getSelectionModel().clearSelection();
        
        // Update final state (but skip instruction highlighting since we're done)
        updateDebugDisplayForCompletion();
        
        // Show completion dialog
        String message = "Debug execution completed!\n";
        if (currentDebugContext.getResult() != null) {
            // Calculate the actual result for operations like division
            double actualResult = calculateActualResultForDebug(currentDebugContext.getResult());
            message += "Result Y = " + formatNumber(actualResult) + "\n";
        }
        
        // For division operations, show both debug cycles and expected normal cycles
        if (currentLoadedFileName != null && currentLoadedFileName.toLowerCase().contains("divide")) {
            List<Double> inputs = getInputValues();
            if (inputs.size() >= 2) {
                try {
                    RunResult normalResult = engine.runProgram(0, inputs);
                    if (normalResult != null) {
                        long normalCycles = normalResult.getCycles();
                        long debugCycles = currentDebugContext.getCycles();
                        
                        message += "Debug cycles: " + debugCycles + " (high-level instructions)\n";
                        message += "Normal cycles: " + normalCycles + " (full algorithm execution)\n";
                        message += "\nNote: Debug mode shows high-level instruction steps.\n";
                        message += "Normal execution expands loops and function calls.";
                    } else {
                        message += "Total cycles: " + currentDebugContext.getCycles();
                    }
                } catch (Exception e) {
                    message += "Total cycles: " + currentDebugContext.getCycles();
                }
            } else {
                message += "Total cycles: " + currentDebugContext.getCycles();
            }
        } else {
            message += "Total cycles: " + currentDebugContext.getCycles();
        }
        
        showInfoDialog("Debug Complete", message);
        
        // Update history table
        updateHistoryTable();
        
        // Reset debug state
        currentDebugContext = null;
        debugPaused = false;
        isExecuting = false;
        updateDebugControls(false);
        debugStatusLabel.setText("Ready");
    }

    // History management methods
    private void onHistoryRowSelected(HistoryRow selectedRow) {
        // Enable context-sensitive actions when a history row is selected
        // Could be used to enable/disable buttons, but for now just acknowledge selection
        System.out.println("Selected history run #" + selectedRow.getRunNumber());
    }
    private List<Double> parseInputs(String inputText) {
        List<Double> inputs = new ArrayList<>();
        if (inputText.isEmpty())
            return inputs;

        String[] parts = inputText.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                try {
                    double value = Double.parseDouble(trimmed);
                    inputs.add(value);
                } catch (NumberFormatException e) {
                    inputs.add(0.0);
                }
            }
        }
        return inputs;
    }

    private void updateVariablesDisplay(Map<String, Double> variables) {
        StringBuilder sb = new StringBuilder();
        
        // Create a copy of variables to potentially modify the 'y' value for division operations
        Map<String, Double> displayVariables = new HashMap<>(variables);
        
        // Check if this is a division operation and update the y value accordingly
        if (currentLoadedFileName != null && currentLoadedFileName.toLowerCase().contains("divide")) {
            System.out.println("DEBUG updateVariablesDisplay: Detected division operation");
            // Calculate the actual decimal result for division
            List<Double> currentInputs = getInputValues();
            if (currentInputs.size() >= 2) {
                double dividend = currentInputs.get(0);  // x1
                double divisor = currentInputs.get(1);   // x2
                if (divisor != 0) {
                    double decimalResult = dividend / divisor;
                    System.out.println("DEBUG updateVariablesDisplay: Updating y from " + variables.get("y") + " to " + decimalResult);
                    displayVariables.put("y", decimalResult);
                }
            }
        }

        displayVariables.entrySet().stream()
                .sorted((a, b) -> {
                    String va = a.getKey();
                    String vb = b.getKey();
                    if (va.equals("y") && !vb.equals("y"))
                        return -1;
                    if (vb.equals("y") && !va.equals("y"))
                        return 1;
                    if (va.startsWith("x") && vb.startsWith("x")) {
                        return Integer.compare(getVariableIndex(va), getVariableIndex(vb));
                    }
                    if (va.startsWith("x"))
                        return -1;
                    if (vb.startsWith("x"))
                        return 1;
                    if (va.startsWith("z") && vb.startsWith("z")) {
                        return Integer.compare(getVariableIndex(va), getVariableIndex(vb));
                    }
                    if (va.startsWith("z"))
                        return 1;
                    if (vb.startsWith("z"))
                        return -1;
                    return va.compareTo(vb);
                })
                .forEach(entry -> sb.append(entry.getKey()).append(" = ").append(formatNumber(entry.getValue())).append("\n"));

        variablesArea.setText(sb.toString());
    }

    /**
     * Special method for updating variables display during debug execution.
     * Uses original input values for division operations instead of modified debug variables.
     */
    private void updateDebugVariablesDisplay(Map<String, Double> debugVariables) {
        StringBuilder sb = new StringBuilder();
        
        // Create a copy of debug variables to potentially modify values for better display
        Map<String, Double> displayVariables = new HashMap<>(debugVariables);
        
        // Check if this is a division operation
        if (currentLoadedFileName != null && currentLoadedFileName.toLowerCase().contains("divide")) {
            System.out.println("DEBUG updateDebugVariablesDisplay: Detected division operation in debug mode");
            
            // For division, we want to show:
            // 1. Original input values for x1 and x2 (preserved)
            // 2. Progressive y calculation based on current z4 (quotient counter)
            List<Double> currentInputs = getInputValues();
            if (currentInputs.size() >= 2) {
                double dividend = currentInputs.get(0);  // x1 from UI
                double divisor = currentInputs.get(1);   // x2 from UI
                
                // Always restore the original input values for x1 and x2
                displayVariables.put("x1", dividend);
                displayVariables.put("x2", divisor);
                System.out.println("DEBUG updateDebugVariablesDisplay: Restored x1=" + dividend + ", x2=" + divisor);
                
                // For Y value: show progressive calculation based on algorithm state
                Double currentZ4 = debugVariables.get("z4");  // quotient counter
                Double currentZ3 = debugVariables.get("z3");  // remaining value to be divided
                Double currentY = debugVariables.get("y");    // algorithm's Y value
                
                if (divisor != 0) {
                    // Show progressive calculation based on current algorithm state
                    if (currentY != null && currentY > 0) {
                        // Algorithm has completed and set Y value, show decimal equivalent
                        double finalDecimalResult = dividend / divisor;
                        displayVariables.put("y", finalDecimalResult);
                        System.out.println("DEBUG updateDebugVariablesDisplay: Algorithm completed, final Y=" + finalDecimalResult);
                    } else if (currentZ4 != null && currentZ4 > 0) {
                        // Show progressive integer value as the quotient builds up
                        // For now, just show the integer progress, decimal will appear at the end
                        displayVariables.put("y", currentZ4);
                        System.out.println("DEBUG updateDebugVariablesDisplay: Progressive integer Y=" + currentZ4 + 
                                         " (z4=" + currentZ4 + ", z3=" + currentZ3 + ")");
                    } else {
                        // Algorithm hasn't started meaningful work yet
                        displayVariables.put("y", 0.0);
                        System.out.println("DEBUG updateDebugVariablesDisplay: Algorithm starting, Y=0");
                    }
                } else {
                    displayVariables.put("y", 0.0);
                    System.out.println("DEBUG updateDebugVariablesDisplay: Division by zero, Y=0");
                }
            }
        }

        displayVariables.entrySet().stream()
                .sorted((a, b) -> {
                    String va = a.getKey();
                    String vb = b.getKey();
                    if (va.equals("y") && !vb.equals("y"))
                        return -1;
                    if (vb.equals("y") && !va.equals("y"))
                        return 1;
                    if (va.startsWith("x") && vb.startsWith("x")) {
                        return Integer.compare(getVariableIndex(va), getVariableIndex(vb));
                    }
                    if (va.startsWith("x"))
                        return -1;
                    if (vb.startsWith("x"))
                        return 1;
                    if (va.startsWith("z") && vb.startsWith("z")) {
                        return Integer.compare(getVariableIndex(va), getVariableIndex(vb));
                    }
                    if (va.startsWith("z"))
                        return 1;
                    if (vb.startsWith("z"))
                        return -1;
                    return va.compareTo(vb);
                })
                .forEach(entry -> sb.append(entry.getKey()).append(" = ").append(formatNumber(entry.getValue())).append("\n"));

        variablesArea.setText(sb.toString());
    }

    private String formatNumber(double value) {
        // Debug output to see what value we're formatting
        System.out.println("DEBUG formatNumber: input value = " + value);
        System.out.println("DEBUG formatNumber: Math.floor(value) = " + Math.floor(value));
        System.out.println("DEBUG formatNumber: value == Math.floor(value) = " + (value == Math.floor(value)));
        
        // Handle special cases first
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            System.out.println("DEBUG formatNumber: returning special case = " + String.valueOf(value));
            return String.valueOf(value);
        }
        
        // Handle very large numbers with scientific notation
        if (Math.abs(value) >= 1e12) {
            String result = String.format("%.2e", value);
            System.out.println("DEBUG formatNumber: returning scientific notation = " + result);
            return result;
        }
        
        // If it's a whole number, show as integer
        if (value == Math.floor(value)) {
            String result = String.format("%.0f", value);
            System.out.println("DEBUG formatNumber: returning integer format = " + result);
            return result;
        } else {
            // For decimal numbers, show appropriate precision based on size
            String formatted;
            if (Math.abs(value) >= 1000) {
                // For large numbers, show fewer decimal places
                formatted = String.format("%.3f", value);
            } else if (Math.abs(value) >= 100) {
                // Medium numbers get 4 decimal places
                formatted = String.format("%.4f", value);
            } else {
                // Small numbers get full precision up to 6 decimal places
                formatted = String.format("%.6f", value);
            }
            
            // Remove trailing zeros after decimal point
            formatted = formatted.replaceAll("0+$", "");
            // Remove trailing decimal point if all decimals were zeros
            formatted = formatted.replaceAll("\\.$", "");
            
            System.out.println("DEBUG formatNumber: returning decimal format = " + formatted);
            return formatted;
        }
    }
    
    /**
     * Calculate the actual mathematical result for operations that should show decimal results.
     * This handles division operations that return integer quotients but should display decimal results.
     */
    private double calculateActualResult(RunResult result) {
        // Debug output
        System.out.println("DEBUG calculateActualResult: currentLoadedFileName = " + currentLoadedFileName);
        
        // Check if this is a division operation by examining the loaded program name
        if (currentLoadedFileName != null && currentLoadedFileName.toLowerCase().contains("divide")) {
            System.out.println("DEBUG: Detected division operation");
            // For division, calculate the true decimal result
            List<Double> currentInputs = getInputValues();
            System.out.println("DEBUG: Current inputs = " + currentInputs);
            if (currentInputs.size() >= 2) {
                double dividend = currentInputs.get(0);  // x1
                double divisor = currentInputs.get(1);   // x2
                System.out.println("DEBUG: dividend=" + dividend + ", divisor=" + divisor);
                if (divisor != 0) {
                    double decimalResult = dividend / divisor;
                    System.out.println("DEBUG: Returning decimal result = " + decimalResult + " instead of integer result = " + result.getY());
                    return decimalResult;  // Return actual decimal division
                }
            }
        }
        
        // For all other operations, return the original result
        System.out.println("DEBUG: Returning original result = " + result.getY());
        return result.getY();
    }
    
    /**
     * Calculate the actual mathematical result for debug operations.
     */
    private double calculateActualResultForDebug(Double debugResult) {
        System.out.println("DEBUG calculateActualResultForDebug: currentLoadedFileName = " + currentLoadedFileName);
        
        // Check if this is a division operation by examining the loaded program name
        if (currentLoadedFileName != null && currentLoadedFileName.toLowerCase().contains("divide")) {
            System.out.println("DEBUG: Detected division operation in debug mode");
            // For division, calculate the true decimal result using original input values
            // NOTE: We must use getInputValues() to get original inputs, not debug context variables
            // because the debug context variables get modified during algorithm execution
            List<Double> currentInputs = getInputValues();
            System.out.println("DEBUG: Original inputs from UI = " + currentInputs);
            if (currentInputs.size() >= 2) {
                double dividend = currentInputs.get(0);  // x1
                double divisor = currentInputs.get(1);   // x2
                System.out.println("DEBUG: debug dividend=" + dividend + ", divisor=" + divisor);
                if (divisor != 0) {
                    double decimalResult = dividend / divisor;
                    System.out.println("DEBUG: Returning debug decimal result = " + decimalResult + " instead of integer result = " + debugResult);
                    return decimalResult;  // Return actual decimal division
                }
            }
        }
        
        // For all other operations, return the original result
        System.out.println("DEBUG: Returning original debug result = " + debugResult);
        return debugResult != null ? debugResult : 0.0;
    }

    private int getVariableIndex(String variable) {
        try {
            if (variable.length() >= 2) {
                return Integer.parseInt(variable.substring(1));
            }
        } catch (NumberFormatException e) {
            // Ignore
        }
        return Integer.MAX_VALUE;
    }

    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        try {
            System.out.println("Starting S-Emulator JavaFX Application...");
            System.out.println("Java Version: " + System.getProperty("java.version"));
            System.out.println("JavaFX Version: " + System.getProperty("javafx.version", "Unknown"));

            launch(args);
        } catch (Exception e) {
            System.err.println("ERROR starting JavaFX application: " + e.getMessage());
            e.printStackTrace();

            // Show error dialog if possible
            try {
                javax.swing.JOptionPane.showMessageDialog(null,
                        "JavaFX Error: " + e.getMessage() + "\n\nTry running: download-javafx21-and-run.bat",
                        "JavaFX Launch Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                System.err.println("Cannot show error dialog: " + ex.getMessage());
            }

            System.exit(1);
        }
    }

    // Data classes for table rows
    public static class InstructionRow {
        private final String number;
        private final String type;
        private final String cycles;
        private final String instruction;
        private boolean highlighted = false;

        public InstructionRow(String number, String type, String cycles, String instruction) {
            this.number = number;
            this.type = type;
            this.cycles = cycles;
            this.instruction = instruction;
        }

        public String getNumber() {
            return number;
        }

        public String getType() {
            return type;
        }

        public String getCycles() {
            return cycles;
        }

        public String getInstruction() {
            return instruction;
        }

        public boolean isHighlighted() {
            return highlighted;
        }

        public void setHighlighted(boolean highlighted) {
            this.highlighted = highlighted;
        }
    }

    public static class HistoryRow {
        private final String runNumber;
        private final String depth;
        private final String yValue;
        private final String cycles;

        public HistoryRow(String runNumber, String depth, String yValue, String cycles) {
            this.runNumber = runNumber;
            this.depth = depth;
            this.yValue = yValue;
            this.cycles = cycles;
        }

        public String getRunNumber() {
            return runNumber;
        }

        public String getDepth() {
            return depth;
        }

        public String getYValue() {
            return yValue;
        }

        public String getCycles() {
            return cycles;
        }
    }
    
    /**
     * Handle theme selection from dropdown
     */
    private void onThemeSelected() {
        String selectedTheme = themeSelector.getValue();
        if (selectedTheme == null) return;
        
        ThemeManager.Theme theme;
        switch (selectedTheme) {
            case "Light": theme = ThemeManager.Theme.LIGHT; break;
            case "Dark": theme = ThemeManager.Theme.DARK; break;
            case "Blue": theme = ThemeManager.Theme.BLUE; break;
            case "Green": theme = ThemeManager.Theme.GREEN; break;
            case "Purple": theme = ThemeManager.Theme.PURPLE; break;
            default: theme = ThemeManager.Theme.LIGHT; break;
        }
        
        themeManager.setTheme(theme);
        applyCurrentTheme();
    }
    
    /**
     * Handle animation type selection from dropdown
     */
    private void onAnimationTypeSelected() {
        String selectedAnimationName = animationTypeSelector.getValue();
        if (selectedAnimationName == null) return;
        
        AnimationManager.AnimationType animationType;
        switch (selectedAnimationName) {
            case "Fade In": animationType = AnimationManager.AnimationType.FADE_IN; break;
            case "Slide In": animationType = AnimationManager.AnimationType.SLIDE_IN; break;
            case "Scale Up": animationType = AnimationManager.AnimationType.SCALE_UP; break;
            default: animationType = AnimationManager.AnimationType.FADE_IN; break;
        }
        
        animationManager.setAnimationType(animationType);
    }
    
    /**
     * Toggle between themes (for backward compatibility)
     */
    private void toggleTheme() {
        themeManager.toggleTheme();
        // Update the ComboBox to reflect the new theme
        String themeName = themeManager.getThemeDisplayName(themeManager.getCurrentTheme());
        themeSelector.setValue(themeName);
        applyCurrentTheme();
    }
    
    /**
     * Apply the current theme to all UI components
     */
    private void applyCurrentTheme() {
        // Apply theme to main containers
        if (mainRoot != null) {
            mainRoot.setStyle(themeManager.getMainBackgroundStyle());
        }
        
        if (topContainer != null) {
            topContainer.setStyle(themeManager.getAccent1BackgroundStyle());
        }
        
        if (instructionsPanel != null) {
            instructionsPanel.setStyle(themeManager.getSecondaryBackgroundStyle());
            // Update instructions panel internal styling
            applyInstructionsPanelTheme();
        }
        
        if (historyChainPanel != null) {
            historyChainPanel.setStyle(themeManager.getAccent2BackgroundStyle());
            applyHistoryChainPanelTheme();
        }
        
        if (debuggerPanel != null) {
            debuggerPanel.setStyle(themeManager.getSecondaryBackgroundStyle());
            applyDebuggerPanelTheme();
        }
        
        if (historyStatsPanel != null) {
            historyStatsPanel.setStyle(themeManager.getAccent2BackgroundStyle());
            applyHistoryStatsPanelTheme();
        }
        
        // Apply theme to individual controls
        applyControlThemes();
    }
    
    private void applyInstructionsPanelTheme() {
        if (instructionsPanel != null && instructionsPanel.getChildren().size() > 0) {
            // Instructions table container
            if (instructionsPanel.getChildren().get(0) instanceof VBox) {
                VBox tableContainer = (VBox) instructionsPanel.getChildren().get(0);
                tableContainer.setStyle(themeManager.getAccent2BackgroundStyle() + " -fx-padding: 20;");
                
                // Apply theme to table title and header labels
                for (int i = 0; i < tableContainer.getChildren().size(); i++) {
                    if (tableContainer.getChildren().get(i) instanceof Label) {
                        Label label = (Label) tableContainer.getChildren().get(i);
                        if (i < 2) { // Title and header labels
                            label.setStyle(label.getStyle() + themeManager.getPrimaryTextStyle());
                        }
                    }
                }
            }
            
            // Summary label
            if (summaryLabel != null) {
                summaryLabel.setStyle(themeManager.getAccent3BackgroundStyle() + 
                    " -fx-padding: 10; -fx-font-weight: bold; -fx-alignment: center;" + 
                    themeManager.getPrimaryTextStyle());
            }
        }
    }
    
    private void applyHistoryChainPanelTheme() {
        if (historyChainPanel != null && instructionHistoryArea != null) {
            // Apply theme to history area
            instructionHistoryArea.setStyle(themeManager.getAccent2BackgroundStyle() + 
                themeManager.getPrimaryTextStyle());
                
            // Apply theme to title
            if (historyChainPanel.getChildren().size() > 0 && 
                historyChainPanel.getChildren().get(0) instanceof Label) {
                Label title = (Label) historyChainPanel.getChildren().get(0);
                title.setStyle(title.getStyle() + themeManager.getPrimaryTextStyle());
            }
        }
    }
    
    private void applyDebuggerPanelTheme() {
        if (debuggerPanel != null) {
            // Apply theme to panel title
            if (debuggerPanel.getChildren().size() > 0 && 
                debuggerPanel.getChildren().get(0) instanceof Label) {
                Label title = (Label) debuggerPanel.getChildren().get(0);
                title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;" + 
                    themeManager.getPrimaryTextStyle());
            }
            
            // Apply theme to command box
            if (debuggerPanel.getChildren().size() > 1 && 
                debuggerPanel.getChildren().get(1) instanceof VBox) {
                VBox commandsBox = (VBox) debuggerPanel.getChildren().get(1);
                commandsBox.setStyle(themeManager.getAccent4BackgroundStyle());
                
                // Apply theme to command labels
                for (int i = 0; i < commandsBox.getChildren().size(); i++) {
                    if (commandsBox.getChildren().get(i) instanceof Label) {
                        Label label = (Label) commandsBox.getChildren().get(i);
                        if (i < 2) { // Command title and list labels
                            label.setStyle(label.getStyle() + themeManager.getPrimaryTextStyle());
                        }
                    }
                }
            }
            
            // Apply theme to variables and execution areas
            if (variablesArea != null) {
                // Use the dedicated method for high-contrast Variables text area
                variablesArea.setStyle(themeManager.getVariablesAreaStyle());
            }
            
            if (executionInputsArea != null) {
                executionInputsArea.setStyle(themeManager.getAccent7BackgroundStyle() + 
                    themeManager.getPrimaryTextStyle());
            }
            
            if (cyclesLabel != null) {
                cyclesLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            }
        }
    }
    
    private void applyHistoryStatsPanelTheme() {
        if (historyStatsPanel != null && historyTable != null) {
            historyTable.setStyle(themeManager.getAccent2BackgroundStyle() + 
                themeManager.getPrimaryTextStyle());
                
            // Apply theme to title
            if (historyStatsPanel.getChildren().size() > 0 && 
                historyStatsPanel.getChildren().get(0) instanceof Label) {
                Label title = (Label) historyStatsPanel.getChildren().get(0);
                title.setStyle(title.getStyle() + themeManager.getPrimaryTextStyle());
            }
        }
    }
    
    private void applyControlThemes() {
        // Update file path label
        if (filePathLabel != null) {
            filePathLabel.setStyle(themeManager.getAccent8BackgroundStyle());
        }
        
        // Update program selector
        if (programSelector != null) {
            programSelector.setStyle(themeManager.getComboBoxStyle());
        }
        
        // Update degree label
        if (degreeLabel != null) {
            degreeLabel.setStyle(themeManager.getDegreeInfoStyle());
        }
        
        // Update highlight field
        if (highlightField != null) {
            highlightField.setStyle(themeManager.getTextInputStyle());
        }
        
        // Update theme selector
        if (themeSelector != null) {
            themeSelector.setStyle(themeManager.getComboBoxStyle());
        }
        
        // Update animation controls
        if (animationsEnabledCheck != null) {
            animationsEnabledCheck.setStyle(themeManager.getPrimaryTextStyle());
        }
        
        if (animationTypeSelector != null) {
            animationTypeSelector.setStyle(themeManager.getComboBoxStyle());
        }
        
        if (splashEnabledCheck != null) {
            splashEnabledCheck.setStyle(themeManager.getPrimaryTextStyle());
        }
        
        if (viewToggleBtn != null) {
            viewToggleBtn.setStyle(themeManager.getInfoButtonStyle());
        }
        
        // Update TreeTableView styling for proper text visibility
        if (instructionsTreeTable != null) {
            applyTreeTableTheme();
            // Refresh row factory to apply new theme-aware colors
            setupTreeTableRowFactory();
        }
        
        // Update button styles - find and update all buttons in top container
        updateButtonStyles(topContainer);
        updateButtonStyles(debuggerPanel);
    }
    
    private void updateButtonStyles(VBox container) {
        if (container == null) return;
        
        updateButtonStylesInNode(container);
    }
    
    private void updateButtonStylesInNode(javafx.scene.Node node) {
        if (node instanceof Button) {
            Button btn = (Button) node;
            String text = btn.getText();
            
            // Apply appropriate theme based on button text/function
            if (text.equals("Load File")) {
                btn.setStyle(themeManager.getSuccessButtonStyle());
            } else if (text.contains("Program Editor") || text.contains("🦫")) {
                btn.setStyle(themeManager.getWarningButtonStyle());
            } else if (text.equals("Collapse") || text.equals("Expand")) {
                btn.setStyle(themeManager.getInfoButtonStyle());
            } else if (text.equals("Highlight")) {
                btn.setStyle(themeManager.getWarningButtonStyle());
            } else if (text.equals("Clear")) {
                btn.setStyle(themeManager.getSecondaryButtonStyle());
            } else if (text.equals("New Run")) {
                btn.setStyle(themeManager.getLightButtonStyle());
            } else if (text.equals("Run")) {
                btn.setStyle(themeManager.getSkyBlueButtonStyle());
            } else {
                // Default button style
                btn.setStyle(themeManager.getInfoButtonStyle());
            }
        } else if (node instanceof javafx.scene.Parent) {
            javafx.scene.Parent parent = (javafx.scene.Parent) node;
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                updateButtonStylesInNode(child);
            }
        }
    }
    
    /**
     * Open the Diligent Program Editor window
     */
    private void openProgramEditor() {
        if (programEditor == null) {
            programEditor = new ProgramEditor(themeManager, this);
        }
        programEditor.show();
    }
    
    /**
     * Load a program file into the emulator (called by Program Editor)
     */
    public void loadProgramFile(File file) {
        if (file != null && file.exists()) {
            // Use existing file loading logic with absolute path
            loadFileWithProgress(file.getAbsolutePath());
        }
    }
}