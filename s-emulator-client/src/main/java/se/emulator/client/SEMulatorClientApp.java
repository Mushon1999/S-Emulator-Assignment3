package se.emulator.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import se.emulator.server.*;
import se.emulator.console.ThemeManager;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main client application for the S-Emulator system.
 * Provides login, program management, and execution capabilities.
 */
public class SEMulatorClientApp extends Application {
    private Stage primaryStage;
    private String currentUser;
    private ServerCommunicationService serverService;
    private ThemeManager themeManager;
    
    // UI Components
    private TabPane mainTabPane;
    private LoginScreen loginScreen;
    private ProgramManagementScreen programManagementScreen;
    private ExecutionScreen executionScreen;
    private ChatScreen chatScreen;
    
    // Real-time update service
    private ScheduledExecutorService updateService;
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.serverService = new ServerCommunicationService();
        this.themeManager = new ThemeManager();
        
        setupUI();
        setupRealTimeUpdates();
        
        primaryStage.setTitle("S-Emulator Client");
        primaryStage.setScene(createMainScene());
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(800);
        primaryStage.show();
    }
    
    private void setupUI() {
        mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Create screens
        loginScreen = new LoginScreen(this);
        programManagementScreen = new ProgramManagementScreen(this);
        executionScreen = new ExecutionScreen(this);
        chatScreen = new ChatScreen(this);
        
        // Add tabs
        mainTabPane.getTabs().addAll(
            new Tab("Login", loginScreen.getContent()),
            new Tab("Program Management", programManagementScreen.getContent()),
            new Tab("Execution", executionScreen.getContent()),
            new Tab("Chat", chatScreen.getContent())
        );
        
        // Initially show only login tab
        showLoginScreen();
    }
    
    private Scene createMainScene() {
        Scene scene = new Scene(mainTabPane);
        themeManager.applyTheme(scene, ThemeManager.Theme.LIGHT);
        return scene;
    }
    
    private void setupRealTimeUpdates() {
        updateService = Executors.newScheduledThreadPool(1);
        updateService.scheduleAtFixedRate(this::updateRealTimeData, 0, 500, TimeUnit.MILLISECONDS);
    }
    
    private void updateRealTimeData() {
        Platform.runLater(() -> {
            if (currentUser != null) {
                programManagementScreen.updateData();
                chatScreen.updateMessages();
            }
        });
    }
    
    public void loginUser(String username) {
       // if (serverService.loginUser(username)) {
         //   currentUser = username;
            showMainScreens();
            programManagementScreen.setCurrentUser(username);
            executionScreen.setCurrentUser(username);
            chatScreen.setCurrentUser(username);
       // } else {
         //   showAlert("Login Failed", "Username already exists. Please choose a different username.");
       // }
    }
    
    public void logoutUser() {
        currentUser = null;
        showLoginScreen();
        programManagementScreen.clearData();
        executionScreen.clearData();
        chatScreen.clearData();
    }
    
    private void showLoginScreen() {
        mainTabPane.getTabs().get(0).setDisable(false);
        mainTabPane.getTabs().get(1).setDisable(true);
        mainTabPane.getTabs().get(2).setDisable(true);
        mainTabPane.getTabs().get(3).setDisable(true);
        mainTabPane.getSelectionModel().select(0);
    }
    
    private void showMainScreens() {
        mainTabPane.getTabs().get(0).setDisable(true);
        mainTabPane.getTabs().get(1).setDisable(false);
        mainTabPane.getTabs().get(2).setDisable(false);
        mainTabPane.getTabs().get(3).setDisable(false);
        mainTabPane.getSelectionModel().select(1);
    }
    
    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Getters for screens
    public ServerCommunicationService getServerService() { return serverService; }
    public String getCurrentUser() { return currentUser; }
    public ThemeManager getThemeManager() { return themeManager; }
    public Stage getPrimaryStage() { return primaryStage; }
    public TabPane getMainTabPane() { return mainTabPane; }
    public ExecutionScreen getExecutionScreen() { return executionScreen; }
    
    @Override
    public void stop() {
        if (updateService != null) {
            updateService.shutdown();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
