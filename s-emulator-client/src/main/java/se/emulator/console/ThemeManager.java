package se.emulator.console;

import javafx.scene.Scene;

/**
 * Simple theme manager for the S-Emulator client.
 */
public class ThemeManager {
    
    public enum Theme {
        LIGHT, DARK
    }
    
    private Theme currentTheme = Theme.LIGHT;
    
    public void applyTheme(Scene scene, Theme theme) {
        this.currentTheme = theme;
        if (theme == Theme.DARK) {
            scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("light-theme.css").toExternalForm());
        }
    }
    
    public Theme getCurrentTheme() {
        return currentTheme;
    }
}
