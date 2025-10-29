package se.emulator.console;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Animated splash screen for S-Emulator application
 * Shows loading progress, animated logo, and smooth transitions
 */
public class SplashScreen {
    
    private Stage splashStage;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Text appTitle;
    private Circle logoCircle;
    private Rectangle logoRect;
    private VBox logoContainer;
    private boolean animationsEnabled = true;
    
    // Animation timelines
    private Timeline progressTimeline;
    private Timeline statusTimeline;
    
    public SplashScreen() {
        // Load splash screen settings
        loadSplashSettings();
    }
    
    /**
     * Show the splash screen and start loading animations
     */
    public void show(Runnable onComplete) {
        if (!animationsEnabled) {
            // Skip splash screen if disabled
            if (onComplete != null) {
                Platform.runLater(onComplete);
            }
            return;
        }
        
        createSplashStage();
        
        // Show splash screen
        splashStage.show();
        
        // Start loading simulation and animations
        startLoadingAnimation(onComplete);
    }
    
    /**
     * Create the splash screen stage and UI
     */
    private void createSplashStage() {
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        splashStage.setResizable(false);
        splashStage.setAlwaysOnTop(true);
        
        // Create main layout
        StackPane root = new StackPane();
        root.setPrefSize(500, 350);
        
        // Create gradient background
        LinearGradient gradient = new LinearGradient(
            0, 0, 1, 1, true, null,
            new Stop(0, Color.web("#667eea")),
            new Stop(1, Color.web("#764ba2"))
        );
        
        Rectangle background = new Rectangle(500, 350);
        background.setFill(gradient);
        
        // Create main content container
        VBox mainContent = new VBox(20);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(40));
        
        // Create animated logo
        createAnimatedLogo();
        
        // Create app title
        appTitle = new Text("S-Emulator");
        appTitle.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        appTitle.setFill(Color.WHITE);
        appTitle.setOpacity(0); // Start invisible for animation
        
        // Add drop shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setOffsetX(3);
        shadow.setOffsetY(3);
        shadow.setColor(Color.web("#00000080"));
        appTitle.setEffect(shadow);
        
        // Create subtitle
        Label subtitle = new Label("Advanced Function Emulator");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        subtitle.setTextFill(Color.web("#E0E0E0"));
        subtitle.setOpacity(0); // Start invisible for animation
        
        // Create progress bar
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        progressBar.setPrefHeight(8);
        progressBar.setStyle(
            "-fx-accent: #4CAF50; " +
            "-fx-background-color: rgba(255,255,255,0.3); " +
            "-fx-background-radius: 4; " +
            "-fx-accent-radius: 4;"
        );
        progressBar.setOpacity(0); // Start invisible for animation
        
        // Create status label
        statusLabel = new Label("Initializing...");
        statusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        statusLabel.setTextFill(Color.web("#F0F0F0"));
        statusLabel.setOpacity(0); // Start invisible for animation
        
        // Create version label
        Label versionLabel = new Label("Version 2.0");
        versionLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        versionLabel.setTextFill(Color.web("#C0C0C0"));
        versionLabel.setOpacity(0); // Start invisible for animation
        
        // Add components to main content
        mainContent.getChildren().addAll(
            logoContainer,
            appTitle,
            subtitle,
            new Region(), // Spacer
            progressBar,
            statusLabel,
            new Region(), // Spacer
            versionLabel
        );
        
        // Add background and content to root
        root.getChildren().addAll(background, mainContent);
        
        // Create scene
        Scene scene = new Scene(root, 500, 350);
        scene.setFill(Color.TRANSPARENT);
        splashStage.setScene(scene);
        
        // Center the splash screen
        splashStage.centerOnScreen();
        
        // Start entrance animations
        startEntranceAnimations(subtitle, versionLabel);
    }
    
    /**
     * Create animated logo with geometric shapes
     */
    private void createAnimatedLogo() {
        logoContainer = new VBox(5);
        logoContainer.setAlignment(Pos.CENTER);
        
        // Create main logo circle
        logoCircle = new Circle(40);
        logoCircle.setFill(Color.web("#4CAF50"));
        logoCircle.setStroke(Color.WHITE);
        logoCircle.setStrokeWidth(3);
        logoCircle.setOpacity(0);
        logoCircle.setScaleX(0.1);
        logoCircle.setScaleY(0.1);
        
        // Create inner rectangle (represents function/emulation)
        logoRect = new Rectangle(30, 20);
        logoRect.setFill(Color.WHITE);
        logoRect.setArcWidth(4);
        logoRect.setArcHeight(4);
        logoRect.setOpacity(0);
        
        // Stack logo elements
        StackPane logoStack = new StackPane();
        logoStack.getChildren().addAll(logoCircle, logoRect);
        
        logoContainer.getChildren().add(logoStack);
    }
    
    /**
     * Start entrance animations for all elements
     */
    private void startEntranceAnimations(Label subtitle, Label versionLabel) {
        // Logo animation - scale up and fade in
        ScaleTransition logoScale = new ScaleTransition(Duration.millis(800), logoCircle);
        logoScale.setFromX(0.1);
        logoScale.setFromY(0.1);
        logoScale.setToX(1.0);
        logoScale.setToY(1.0);
        logoScale.setInterpolator(Interpolator.EASE_OUT);
        
        FadeTransition logoFade = new FadeTransition(Duration.millis(800), logoCircle);
        logoFade.setFromValue(0);
        logoFade.setToValue(1);
        
        // Logo rect fade in (delayed)
        FadeTransition rectFade = new FadeTransition(Duration.millis(600), logoRect);
        rectFade.setFromValue(0);
        rectFade.setToValue(1);
        rectFade.setDelay(Duration.millis(400));
        
        // Title fade in
        FadeTransition titleFade = new FadeTransition(Duration.millis(800), appTitle);
        titleFade.setFromValue(0);
        titleFade.setToValue(1);
        titleFade.setDelay(Duration.millis(600));
        
        // Subtitle fade in
        FadeTransition subtitleFade = new FadeTransition(Duration.millis(600), subtitle);
        subtitleFade.setFromValue(0);
        subtitleFade.setToValue(1);
        subtitleFade.setDelay(Duration.millis(1000));
        
        // Progress bar fade in
        FadeTransition progressFade = new FadeTransition(Duration.millis(600), progressBar);
        progressFade.setFromValue(0);
        progressFade.setToValue(1);
        progressFade.setDelay(Duration.millis(1200));
        
        // Status label fade in
        FadeTransition statusFade = new FadeTransition(Duration.millis(600), statusLabel);
        statusFade.setFromValue(0);
        statusFade.setToValue(1);
        statusFade.setDelay(Duration.millis(1200));
        
        // Version label fade in
        FadeTransition versionFade = new FadeTransition(Duration.millis(600), versionLabel);
        versionFade.setFromValue(0);
        versionFade.setToValue(1);
        versionFade.setDelay(Duration.millis(1400));
        
        // Play all animations
        ParallelTransition entrance = new ParallelTransition(
            logoScale, logoFade, rectFade, titleFade, 
            subtitleFade, progressFade, statusFade, versionFade
        );
        
        entrance.play();
        
        // Add rotation animation to logo
        startLogoRotationAnimation();
    }
    
    /**
     * Start continuous logo rotation animation
     */
    private void startLogoRotationAnimation() {
        RotateTransition rotate = new RotateTransition(Duration.seconds(3), logoCircle);
        rotate.setFromAngle(0);
        rotate.setToAngle(360);
        rotate.setCycleCount(Timeline.INDEFINITE);
        rotate.setInterpolator(Interpolator.LINEAR);
        rotate.setDelay(Duration.millis(800));
        rotate.play();
    }
    
    /**
     * Start loading progress animation with status updates
     */
    private void startLoadingAnimation(Runnable onComplete) {
        // Simulate loading with realistic progress and status updates
        String[] loadingSteps = {
            "Initializing...",
            "Loading engine components...",
            "Setting up user interface...",
            "Applying themes...",
            "Loading animation system...",
            "Finalizing setup...",
            "Ready!"
        };
        
        Task<Void> loadingTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < loadingSteps.length; i++) {
                    final int step = i;
                    final double progress = (double) i / (loadingSteps.length - 1);
                    
                    Platform.runLater(() -> {
                        progressBar.setProgress(progress);
                        statusLabel.setText(loadingSteps[step]);
                        
                        // Add slight fade animation for status changes
                        FadeTransition statusChange = new FadeTransition(Duration.millis(200), statusLabel);
                        statusChange.setFromValue(0.7);
                        statusChange.setToValue(1.0);
                        statusChange.play();
                    });
                    
                    // Variable delay for realistic loading
                    Thread.sleep(i == 0 ? 500 : (200 + (int)(Math.random() * 300)));
                }
                
                return null;
            }
        };
        
        loadingTask.setOnSucceeded(e -> {
            // Add completion animation
            FadeTransition completeFade = new FadeTransition(Duration.millis(500), splashStage.getScene().getRoot());
            completeFade.setFromValue(1.0);
            completeFade.setToValue(0.0);
            completeFade.setDelay(Duration.millis(300));
            
            completeFade.setOnFinished(event -> {
                splashStage.close();
                if (onComplete != null) {
                    onComplete.run();
                }
            });
            
            completeFade.play();
        });
        
        // Start loading task
        Thread loadingThread = new Thread(loadingTask);
        loadingThread.setDaemon(true);
        loadingThread.start();
    }
    
    /**
     * Enable or disable splash screen
     */
    public void setSplashEnabled(boolean enabled) {
        this.animationsEnabled = enabled;
        saveSplashSettings();
    }
    
    /**
     * Check if splash screen is enabled
     */
    public boolean isSplashEnabled() {
        return animationsEnabled;
    }
    
    /**
     * Load splash screen settings
     */
    private void loadSplashSettings() {
        try {
            java.util.Properties props = new java.util.Properties();
            java.io.FileInputStream fis = new java.io.FileInputStream("animation_settings.properties");
            props.load(fis);
            fis.close();
            
            String splashEnabledStr = props.getProperty("splash.enabled", "true");
            animationsEnabled = Boolean.parseBoolean(splashEnabledStr);
            
        } catch (Exception e) {
            // File doesn't exist or error reading - use default
            animationsEnabled = true;
        }
    }
    
    /**
     * Save splash screen settings
     */
    private void saveSplashSettings() {
        try {
            java.util.Properties props = new java.util.Properties();
            
            // Load existing properties first
            try {
                java.io.FileInputStream fis = new java.io.FileInputStream("animation_settings.properties");
                props.load(fis);
                fis.close();
            } catch (Exception e) {
                // File doesn't exist - will create new
            }
            
            // Update splash setting
            props.setProperty("splash.enabled", String.valueOf(animationsEnabled));
            
            // Save properties
            java.io.FileOutputStream fos = new java.io.FileOutputStream("animation_settings.properties");
            props.store(fos, "S-Emulator Animation Settings");
            fos.close();
            
        } catch (Exception e) {
            System.err.println("Could not save splash settings: " + e.getMessage());
        }
    }
}