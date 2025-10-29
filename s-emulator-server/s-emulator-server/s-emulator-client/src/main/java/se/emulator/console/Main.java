package se.emulator.console;

import javafx.application.Application;

public final class Main {
    public static void main(String[] args) {
        // Launch the splash launcher which will handle splash screen and main app
        Application.launch(SplashLauncher.class, args);
    }
}