package se.emulator.console;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import se.emulator.engine.*;

/**
 * Splash Launcher - Handles splash screen display and main application launch
 */
public class SplashLauncher extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Don't use the primary stage - we'll create our own
        primaryStage.close();
        
        // Show splash screen first
        SplashScreen splash = new SplashScreen();
        splash.show(() -> {
            // After splash screen completes, launch main application
            Platform.runLater(() -> {
                try {
                    SEmulatorApp mainApp = new SEmulatorApp();
                    Stage mainStage = new Stage();
                    mainApp.start(mainStage);
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.exit();
                }
            });
        });
    }
    
    @Override
    public void stop() throws Exception {
        super.stop();
        Platform.exit();
        System.exit(0);
    }
}