package se.emulator.client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import se.emulator.console.ThemeManager;

/**
 * Login screen for user authentication.
 */
public class LoginScreen {
    private final SEMulatorClientApp app;
    private final VBox content;
    private TextField usernameField;
    private Button loginButton;
    private Button rechargeButton;
    private TextField rechargeAmountField;
    
    public LoginScreen(SEMulatorClientApp app) {
        this.app = app;
        this.content = createContent();
    }
    
    private VBox createContent() {
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(50));
        
        // Title
        Label titleLabel = new Label("S-Emulator Client");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        // Username input
        VBox usernameContainer = new VBox(10);
        Label usernameLabel = new Label("Username:");
        usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setMaxWidth(300);
        usernameContainer.getChildren().addAll(usernameLabel, usernameField);
        
        // Login button
        loginButton = new Button("Login");
        loginButton.setPrefWidth(150);
        loginButton.setOnAction(e -> handleLogin());
        
        // Recharge section
        VBox rechargeContainer = new VBox(10);
        Label rechargeLabel = new Label("Recharge Credits (for existing users):");
        HBox rechargeBox = new HBox(10);
        rechargeAmountField = new TextField();
        rechargeAmountField.setPromptText("Amount");
        rechargeAmountField.setMaxWidth(100);
        rechargeButton = new Button("Recharge");
        rechargeButton.setOnAction(e -> handleRecharge());
        rechargeBox.getChildren().addAll(rechargeAmountField, rechargeButton);
        rechargeContainer.getChildren().addAll(rechargeLabel, rechargeBox);
        
        // Instructions
        Label instructionsLabel = new Label(
            "Enter a unique username to login to the S-Emulator system.\n" +
            "You will start with 1000 credits. Use the recharge option if you need more."
        );
        instructionsLabel.setStyle("-fx-font-size: 12px; -fx-text-alignment: center;");
        instructionsLabel.setWrapText(true);
        
        mainContainer.getChildren().addAll(
            titleLabel,
            usernameContainer,
            loginButton,
            new Separator(),
            rechargeContainer,
            instructionsLabel
        );
        
        // Handle Enter key for login
        usernameField.setOnAction(e -> handleLogin());
        
        return mainContainer;
    }
    
    private void handleLogin() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            app.showErrorAlert("Invalid Username", "Please enter a username.");
            return;
        }
        
        app.loginUser(username);
    }
    
    private void handleRecharge() {
        String username = usernameField.getText().trim();
        String amountText = rechargeAmountField.getText().trim();
        
        if (username.isEmpty()) {
            app.showErrorAlert("Invalid Username", "Please enter a username first.");
            return;
        }
        
        try {
            long amount = Long.parseLong(amountText);
            if (amount <= 0) {
                app.showErrorAlert("Invalid Amount", "Please enter a positive amount.");
                return;
            }
            
            app.getServerService().rechargeUser(username, amount);
            app.showAlert("Recharge Successful", "Credits added to your account.");
        } catch (NumberFormatException e) {
            app.showErrorAlert("Invalid Amount", "Please enter a valid number.");
        }
    }
    
    public VBox getContent() {
        return content;
    }
}
