package se.emulator.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import se.emulator.server.ChatMessage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Chat screen for real-time communication between users.
 */
public class ChatScreen {
    private final SEMulatorClientApp app;
    private final VBox content;
    private String currentUser;
    
    // UI Components
    private TableView<ChatMessage> messagesTable;
    private TextField messageField;
    private Button sendButton;
    private Button refreshButton;
    
    public ChatScreen(SEMulatorClientApp app) {
        this.app = app;
        this.content = createContent();
    }
    
    private VBox createContent() {
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        
        // Messages table
        VBox messagesBox = createMessagesSection();
        
        // Message input section
        VBox inputBox = createInputSection();
        
        mainContainer.getChildren().addAll(messagesBox, inputBox);
        return mainContainer;
    }
    
    private VBox createMessagesSection() {
        VBox container = new VBox(10);
        
        // Messages table
        messagesTable = new TableView<>();
        messagesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        messagesTable.setPrefHeight(400);
        
        TableColumn<ChatMessage, String> senderCol = new TableColumn<>("Sender");
        senderCol.setCellValueFactory(new PropertyValueFactory<>("sender"));
        senderCol.setPrefWidth(120);
        
        TableColumn<ChatMessage, String> messageCol = new TableColumn<>("Message");
        messageCol.setCellValueFactory(new PropertyValueFactory<>("message"));
        
        TableColumn<ChatMessage, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData -> {
            long timestamp = cellData.getValue().getTimestamp();
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
            String formattedTime = dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            return new javafx.beans.property.SimpleStringProperty(formattedTime);
        });
        timeCol.setPrefWidth(100);
        
        messagesTable.getColumns().addAll(senderCol, messageCol, timeCol);
        
        // Refresh button
        refreshButton = new Button("Refresh Messages");
        refreshButton.setOnAction(e -> updateMessages());
        
        container.getChildren().addAll(
            new Label("Chat Messages:"),
            messagesTable,
            refreshButton
        );
        
        return container;
    }
    
    private VBox createInputSection() {
        VBox container = new VBox(10);
        
        HBox inputBox = new HBox(10);
        messageField = new TextField();
        messageField.setPromptText("Type your message here...");
        messageField.setOnAction(e -> sendMessage());
        
        sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());
        
        inputBox.getChildren().addAll(messageField, sendButton);
        
        // Instructions
        Label instructionsLabel = new Label(
            "Type a message and press Enter or click Send to communicate with other users."
        );
        instructionsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        
        container.getChildren().addAll(
            new Label("Send Message:"),
            inputBox,
            instructionsLabel
        );
        
        return container;
    }
    
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        
        if (currentUser == null) {
            app.showErrorAlert("Not Logged In", "Please log in first to send messages.");
            return;
        }
        
        app.getServerService().sendChatMessage(currentUser, message);
        messageField.clear();
        updateMessages();
    }
    
    public void setCurrentUser(String username) {
        this.currentUser = username;
        updateMessages();
    }
    
    public void updateMessages() {
        Platform.runLater(() -> {
            List<ChatMessage> messages = app.getServerService().getChatMessages();
            messagesTable.setItems(FXCollections.observableArrayList(messages));
            
            // Auto-scroll to bottom
            if (!messages.isEmpty()) {
                messagesTable.scrollTo(messages.size() - 1);
            }
        });
    }
    
    public void clearData() {
        messagesTable.setItems(FXCollections.emptyObservableList());
        messageField.clear();
    }
    
    public VBox getContent() {
        return content;
    }
}
