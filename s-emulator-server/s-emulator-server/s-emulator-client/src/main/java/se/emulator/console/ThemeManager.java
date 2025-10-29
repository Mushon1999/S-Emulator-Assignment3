package se.emulator.console;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * ThemeManager handles switching between different visual themes in the application.
 * Supports multiple themes with appropriate color schemes for visibility.
 */
public class ThemeManager {
    
    public enum Theme {
        LIGHT, DARK, BLUE, GREEN, PURPLE
    }
    
    private Theme currentTheme = Theme.LIGHT;
    private List<Themeable> themeableComponents = new ArrayList<>();
    
    // Theme color definitions
    private static class ThemeColors {
        // Light theme colors
        static final String LIGHT_BG_PRIMARY = "#f0f0f0";
        static final String LIGHT_BG_SECONDARY = "#ffffff";
        static final String LIGHT_BG_ACCENT1 = "#f8f9fa";
        static final String LIGHT_BG_ACCENT2 = "#F5DEB3";
        static final String LIGHT_BG_ACCENT3 = "#E6E6FA";
        static final String LIGHT_BG_ACCENT4 = "#FFFF99";
        static final String LIGHT_BG_ACCENT5 = "#FF69B4";
        static final String LIGHT_BG_ACCENT6 = "#4169E1";
        static final String LIGHT_BG_ACCENT7 = "#87CEEB";
        static final String LIGHT_BG_ACCENT8 = "#fff3cd";
        static final String LIGHT_TEXT_PRIMARY = "#212529";
        static final String LIGHT_TEXT_SECONDARY = "#666666";
        static final String LIGHT_TEXT_ACCENT = "white";
        static final String LIGHT_BORDER = "#cccccc";
        static final String LIGHT_BORDER_ACCENT = "#dee2e6";
        
        // Dark theme colors
        static final String DARK_BG_PRIMARY = "#2b2b2b";
        static final String DARK_BG_SECONDARY = "#3c3c3c";
        static final String DARK_BG_ACCENT1 = "#404040";
        static final String DARK_BG_ACCENT2 = "#5d4e37";
        static final String DARK_BG_ACCENT3 = "#4a4a5a";
        static final String DARK_BG_ACCENT4 = "#6b6b00";
        static final String DARK_BG_ACCENT5 = "#8b008b";
        static final String DARK_BG_ACCENT6 = "#191970";
        static final String DARK_BG_ACCENT7 = "#4682b4";
        static final String DARK_BG_ACCENT8 = "#5d5016";
        static final String DARK_TEXT_PRIMARY = "#ffffff";
        static final String DARK_TEXT_SECONDARY = "#cccccc";
        static final String DARK_TEXT_ACCENT = "#ffffff";
        static final String DARK_BORDER = "#555555";
        static final String DARK_BORDER_ACCENT = "#666666";
        
        // Blue theme colors
        static final String BLUE_BG_PRIMARY = "#e8f4f8";
        static final String BLUE_BG_SECONDARY = "#f0f8ff";
        static final String BLUE_BG_ACCENT1 = "#deebf7";
        static final String BLUE_BG_ACCENT2 = "#b3d9ff";
        static final String BLUE_BG_ACCENT3 = "#cce7ff";
        static final String BLUE_BG_ACCENT4 = "#80bfff";
        static final String BLUE_BG_ACCENT5 = "#0066cc";
        static final String BLUE_BG_ACCENT6 = "#004080";
        static final String BLUE_BG_ACCENT7 = "#66a3ff";
        static final String BLUE_BG_ACCENT8 = "#b3d1ff";
        static final String BLUE_TEXT_PRIMARY = "#1a1a1a";
        static final String BLUE_TEXT_SECONDARY = "#4d4d4d";
        static final String BLUE_TEXT_ACCENT = "#ffffff";
        static final String BLUE_BORDER = "#80bfff";
        static final String BLUE_BORDER_ACCENT = "#99ccff";
        
        // Green theme colors
        static final String GREEN_BG_PRIMARY = "#f0f8f0";
        static final String GREEN_BG_SECONDARY = "#f5fff5";
        static final String GREEN_BG_ACCENT1 = "#e8f5e8";
        static final String GREEN_BG_ACCENT2 = "#d4e8d4";
        static final String GREEN_BG_ACCENT3 = "#c8e6c8";
        static final String GREEN_BG_ACCENT4 = "#90ee90";
        static final String GREEN_BG_ACCENT5 = "#228b22";
        static final String GREEN_BG_ACCENT6 = "#006400";
        static final String GREEN_BG_ACCENT7 = "#66cc66";
        static final String GREEN_BG_ACCENT8 = "#ccffcc";
        static final String GREEN_TEXT_PRIMARY = "#1a1a1a";
        static final String GREEN_TEXT_SECONDARY = "#4d4d4d";
        static final String GREEN_TEXT_ACCENT = "#ffffff";
        static final String GREEN_BORDER = "#90ee90";
        static final String GREEN_BORDER_ACCENT = "#b3ffb3";
        
        // Purple theme colors
        static final String PURPLE_BG_PRIMARY = "#f8f0f8";
        static final String PURPLE_BG_SECONDARY = "#fff5ff";
        static final String PURPLE_BG_ACCENT1 = "#f0e8f0";
        static final String PURPLE_BG_ACCENT2 = "#e8d4e8";
        static final String PURPLE_BG_ACCENT3 = "#e6c8e6";
        static final String PURPLE_BG_ACCENT4 = "#dda0dd";
        static final String PURPLE_BG_ACCENT5 = "#8b008b";
        static final String PURPLE_BG_ACCENT6 = "#4b0082";
        static final String PURPLE_BG_ACCENT7 = "#ba55d3";
        static final String PURPLE_BG_ACCENT8 = "#f0d0f0";
        static final String PURPLE_TEXT_PRIMARY = "#1a1a1a";
        static final String PURPLE_TEXT_SECONDARY = "#4d4d4d";
        static final String PURPLE_TEXT_ACCENT = "#ffffff";
        static final String PURPLE_BORDER = "#dda0dd";
        static final String PURPLE_BORDER_ACCENT = "#f0a0f0";
    }
    
    /**
     * Interface for components that can be themed
     */
    public interface Themeable {
        void applyTheme(Theme theme);
    }
    
    /**
     * Register a component to receive theme updates
     */
    public void registerThemeable(Themeable component) {
        themeableComponents.add(component);
    }
    
    /**
     * Switch to the specified theme
     */
    public void setTheme(Theme theme) {
        this.currentTheme = theme;
        applyThemeToAll();
    }
    
    /**
     * Get the current theme
     */
    public Theme getCurrentTheme() {
        return currentTheme;
    }
    
    /**
     * Toggle between light and dark themes
     */
    public void toggleTheme() {
        // Cycle through themes
        switch (currentTheme) {
            case LIGHT: setTheme(Theme.DARK); break;
            case DARK: setTheme(Theme.BLUE); break;
            case BLUE: setTheme(Theme.GREEN); break;
            case GREEN: setTheme(Theme.PURPLE); break;
            case PURPLE: setTheme(Theme.LIGHT); break;
            default: setTheme(Theme.LIGHT); break;
        }
    }
    
    /**
     * Get display name for theme
     */
    public String getThemeDisplayName(Theme theme) {
        switch (theme) {
            case LIGHT: return "Light";
            case DARK: return "Dark";
            case BLUE: return "Blue";
            case GREEN: return "Green";
            case PURPLE: return "Purple";
            default: return "Light";
        }
    }
    
    /**
     * Get all available themes
     */
    public Theme[] getAllThemes() {
        return Theme.values();
    }
    
    /**
     * Get theme colors for current theme
     */
    private String getPrimaryBg() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_BG_PRIMARY;
            case DARK: return ThemeColors.DARK_BG_PRIMARY;
            case BLUE: return ThemeColors.BLUE_BG_PRIMARY;
            case GREEN: return ThemeColors.GREEN_BG_PRIMARY;
            case PURPLE: return ThemeColors.PURPLE_BG_PRIMARY;
            default: return ThemeColors.LIGHT_BG_PRIMARY;
        }
    }
    
    private String getSecondaryBg() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_BG_SECONDARY;
            case DARK: return ThemeColors.DARK_BG_SECONDARY;
            case BLUE: return ThemeColors.BLUE_BG_SECONDARY;
            case GREEN: return ThemeColors.GREEN_BG_SECONDARY;
            case PURPLE: return ThemeColors.PURPLE_BG_SECONDARY;
            default: return ThemeColors.LIGHT_BG_SECONDARY;
        }
    }
    
    private String getAccent1Bg() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_BG_ACCENT1;
            case DARK: return ThemeColors.DARK_BG_ACCENT1;
            case BLUE: return ThemeColors.BLUE_BG_ACCENT1;
            case GREEN: return ThemeColors.GREEN_BG_ACCENT1;
            case PURPLE: return ThemeColors.PURPLE_BG_ACCENT1;
            default: return ThemeColors.LIGHT_BG_ACCENT1;
        }
    }
    
    private String getAccent2Bg() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_BG_ACCENT2;
            case DARK: return ThemeColors.DARK_BG_ACCENT2;
            case BLUE: return ThemeColors.BLUE_BG_ACCENT2;
            case GREEN: return ThemeColors.GREEN_BG_ACCENT2;
            case PURPLE: return ThemeColors.PURPLE_BG_ACCENT2;
            default: return ThemeColors.LIGHT_BG_ACCENT2;
        }
    }
    
    private String getAccent3Bg() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_BG_ACCENT3;
            case DARK: return ThemeColors.DARK_BG_ACCENT3;
            case BLUE: return ThemeColors.BLUE_BG_ACCENT3;
            case GREEN: return ThemeColors.GREEN_BG_ACCENT3;
            case PURPLE: return ThemeColors.PURPLE_BG_ACCENT3;
            default: return ThemeColors.LIGHT_BG_ACCENT3;
        }
    }
    
    private String getAccent4Bg() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_BG_ACCENT4;
            case DARK: return ThemeColors.DARK_BG_ACCENT4;
            case BLUE: return ThemeColors.BLUE_BG_ACCENT4;
            case GREEN: return ThemeColors.GREEN_BG_ACCENT4;
            case PURPLE: return ThemeColors.PURPLE_BG_ACCENT4;
            default: return ThemeColors.LIGHT_BG_ACCENT4;
        }
    }
    
    private String getAccent5Bg() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_BG_ACCENT5;
            case DARK: return ThemeColors.DARK_BG_ACCENT5;
            case BLUE: return ThemeColors.BLUE_BG_ACCENT5;
            case GREEN: return ThemeColors.GREEN_BG_ACCENT5;
            case PURPLE: return ThemeColors.PURPLE_BG_ACCENT5;
            default: return ThemeColors.LIGHT_BG_ACCENT5;
        }
    }
    
    private String getAccent6Bg() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_BG_ACCENT6;
            case DARK: return ThemeColors.DARK_BG_ACCENT6;
            case BLUE: return ThemeColors.BLUE_BG_ACCENT6;
            case GREEN: return ThemeColors.GREEN_BG_ACCENT6;
            case PURPLE: return ThemeColors.PURPLE_BG_ACCENT6;
            default: return ThemeColors.LIGHT_BG_ACCENT6;
        }
    }
    
    private String getAccent7Bg() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_BG_ACCENT7;
            case DARK: return ThemeColors.DARK_BG_ACCENT7;
            case BLUE: return ThemeColors.BLUE_BG_ACCENT7;
            case GREEN: return ThemeColors.GREEN_BG_ACCENT7;
            case PURPLE: return ThemeColors.PURPLE_BG_ACCENT7;
            default: return ThemeColors.LIGHT_BG_ACCENT7;
        }
    }
    
    private String getAccent8Bg() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_BG_ACCENT8;
            case DARK: return ThemeColors.DARK_BG_ACCENT8;
            case BLUE: return ThemeColors.BLUE_BG_ACCENT8;
            case GREEN: return ThemeColors.GREEN_BG_ACCENT8;
            case PURPLE: return ThemeColors.PURPLE_BG_ACCENT8;
            default: return ThemeColors.LIGHT_BG_ACCENT8;
        }
    }
    
    private String getPrimaryText() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_TEXT_PRIMARY;
            case DARK: return ThemeColors.DARK_TEXT_PRIMARY;
            case BLUE: return ThemeColors.BLUE_TEXT_PRIMARY;
            case GREEN: return ThemeColors.GREEN_TEXT_PRIMARY;
            case PURPLE: return ThemeColors.PURPLE_TEXT_PRIMARY;
            default: return ThemeColors.LIGHT_TEXT_PRIMARY;
        }
    }
    
    private String getSecondaryText() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_TEXT_SECONDARY;
            case DARK: return ThemeColors.DARK_TEXT_SECONDARY;
            case BLUE: return ThemeColors.BLUE_TEXT_SECONDARY;
            case GREEN: return ThemeColors.GREEN_TEXT_SECONDARY;
            case PURPLE: return ThemeColors.PURPLE_TEXT_SECONDARY;
            default: return ThemeColors.LIGHT_TEXT_SECONDARY;
        }
    }
    
    private String getAccentText() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_TEXT_ACCENT;
            case DARK: return ThemeColors.DARK_TEXT_ACCENT;
            case BLUE: return ThemeColors.BLUE_TEXT_ACCENT;
            case GREEN: return ThemeColors.GREEN_TEXT_ACCENT;
            case PURPLE: return ThemeColors.PURPLE_TEXT_ACCENT;
            default: return ThemeColors.LIGHT_TEXT_ACCENT;
        }
    }
    
    private String getBorder() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_BORDER;
            case DARK: return ThemeColors.DARK_BORDER;
            case BLUE: return ThemeColors.BLUE_BORDER;
            case GREEN: return ThemeColors.GREEN_BORDER;
            case PURPLE: return ThemeColors.PURPLE_BORDER;
            default: return ThemeColors.LIGHT_BORDER;
        }
    }
    
    private String getBorderAccent() {
        switch (currentTheme) {
            case LIGHT: return ThemeColors.LIGHT_BORDER_ACCENT;
            case DARK: return ThemeColors.DARK_BORDER_ACCENT;
            case BLUE: return ThemeColors.BLUE_BORDER_ACCENT;
            case GREEN: return ThemeColors.GREEN_BORDER_ACCENT;
            case PURPLE: return ThemeColors.PURPLE_BORDER_ACCENT;
            default: return ThemeColors.LIGHT_BORDER_ACCENT;
        }
    }
    
    /**
     * Apply current theme to all registered components
     */
    private void applyThemeToAll() {
        for (Themeable component : themeableComponents) {
            component.applyTheme(currentTheme);
        }
    }
    
    /**
     * Get CSS style for main background
     */
    public String getMainBackgroundStyle() {
        return "-fx-background-color: " + getPrimaryBg() + ";";
    }
    
    /**
     * Get CSS style for secondary background (panels)
     */
    public String getSecondaryBackgroundStyle() {
        return "-fx-background-color: " + getSecondaryBg() + 
            "; -fx-border-color: " + getBorder() + 
            "; -fx-border-width: 2;";
    }
    
    /**
     * Get CSS style for accent background 1 (top controls)
     */
    public String getAccent1BackgroundStyle() {
        return "-fx-background-color: " + getAccent1Bg() + 
            "; -fx-border-color: " + getBorderAccent() + 
            "; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;";
    }
    
    /**
     * Get CSS style for accent background 2 (beige areas - instructions)
     */
    public String getAccent2BackgroundStyle() {
        return "-fx-background-color: " + getAccent2Bg() + 
            "; -fx-border-color: " + getBorder() + 
            "; -fx-border-width: 2;";
    }
    
    /**
     * Get CSS style for accent background 3 (purple/lavender areas)
     */
    public String getAccent3BackgroundStyle() {
        return "-fx-background-color: " + getAccent3Bg() + ";";
    }
    
    /**
     * Get CSS style for accent background 4 (yellow areas)
     */
    public String getAccent4BackgroundStyle() {
        return "-fx-background-color: " + getAccent4Bg() + 
            "; -fx-padding: 15; -fx-border-color: " + getBorder() + "; -fx-border-width: 1;";
    }
    
    /**
     * Get CSS style for accent background 5 (magenta areas)
     */
    public String getAccent5BackgroundStyle() {
        return "-fx-background-color: " + getAccent5Bg() + "; -fx-padding: 10;";
    }
    
    /**
     * Get CSS style for accent background 6 (blue areas)
     */
    public String getAccent6BackgroundStyle() {
        return "-fx-background-color: " + getAccent6Bg() + "; -fx-padding: 10;";
    }
    
    /**
     * Get CSS style for accent background 7 (light blue areas)
     */
    public String getAccent7BackgroundStyle() {
        return "-fx-background-color: " + getAccent7Bg() + ";";
    }
    
    /**
     * Get CSS style for accent background 8 (warning/info areas)
     */
    public String getAccent8BackgroundStyle() {
        String borderColor = currentTheme == Theme.LIGHT ? "#ffeaa7" : getBorderAccent();
        String textColor = currentTheme == Theme.DARK ? "white" : getPrimaryText();
        return "-fx-background-color: " + getAccent8Bg() + 
            "; -fx-padding: 10; -fx-border-color: " + borderColor + 
            "; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px; -fx-text-fill: " + textColor + ";";
    }
    
    /**
     * Get CSS style for primary text
     */
    public String getPrimaryTextStyle() {
        return "-fx-text-fill: " + getPrimaryText() + ";";
    }
    
    /**
     * Get CSS style for secondary text
     */
    public String getSecondaryTextStyle() {
        return "-fx-text-fill: " + getSecondaryText() + ";";
    }
    
    /**
     * Get CSS style for accent text (on colored backgrounds)
     */
    public String getAccentTextStyle() {
        return "-fx-text-fill: " + getAccentText() + ";";
    }
    
    /**
     * Get CSS style for buttons with success color
     */
    public String getSuccessButtonStyle() {
        String baseColor = currentTheme == Theme.DARK ? "#20c997" : "#28a745";
        return "-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-font-weight: bold; " +
               "-fx-padding: 10 20; -fx-border-radius: 4; -fx-background-radius: 4; " +
               "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 1);";
    }
    
    /**
     * Get CSS style for buttons with info color
     */
    public String getInfoButtonStyle() {
        String baseColor = currentTheme == Theme.DARK ? "#20c997" : "#17a2b8";
        return "-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-font-weight: bold; " +
               "-fx-padding: 8 16; -fx-border-radius: 4; -fx-background-radius: 4;";
    }
    
    /**
     * Get CSS style for buttons with warning color
     */
    public String getWarningButtonStyle() {
        String baseColor = "#ffc107";
        String textColor = "#212529";
        return "-fx-background-color: " + baseColor + "; -fx-text-fill: " + textColor + "; -fx-font-weight: bold; " +
               "-fx-padding: 8 12; -fx-border-radius: 4; -fx-background-radius: 4;";
    }
    
    /**
     * Get CSS style for buttons with secondary color
     */
    public String getSecondaryButtonStyle() {
        String baseColor = "#6c757d";
        return "-fx-background-color: " + baseColor + "; -fx-text-fill: white; -fx-font-weight: bold; " +
               "-fx-padding: 8 12; -fx-border-radius: 4; -fx-background-radius: 4;";
    }
    
    /**
     * Get CSS style for buttons with light color
     */
    public String getLightButtonStyle() {
        String baseColor = currentTheme == Theme.DARK ? "#32cd32" : "#90EE90";
        String textColor = currentTheme == Theme.DARK ? "#ffffff" : "#000000";
        return "-fx-background-color: " + baseColor + "; -fx-text-fill: " + textColor + "; -fx-font-weight: bold;";
    }
    
    /**
     * Get CSS style for buttons with sky blue color
     */
    public String getSkyBlueButtonStyle() {
        String baseColor = currentTheme == Theme.DARK ? "#4682b4" : "#87CEEB";
        String textColor = currentTheme == Theme.DARK ? "#ffffff" : "#000000";
        return "-fx-background-color: " + baseColor + "; -fx-text-fill: " + textColor + "; -fx-font-weight: bold;";
    }
    
    /**
     * Get CSS style for text inputs
     */
    public String getTextInputStyle() {
        String borderColor = currentTheme == Theme.DARK ? "#666666" : "#ced4da";
        String bgColor = currentTheme == Theme.DARK ? "#4a4a4a" : "#ffffff";
        String textColor = getPrimaryText();
        return "-fx-padding: 8; -fx-border-color: " + borderColor + "; -fx-border-width: 1; " +
               "-fx-border-radius: 4; -fx-background-radius: 4; -fx-background-color: " + bgColor + 
               "; -fx-text-fill: " + textColor + ";";
    }
    
    /**
     * Get CSS style for labels in degree info
     */
    public String getDegreeInfoStyle() {
        String bgColor = currentTheme == Theme.DARK ? "#555555" : "#e9ecef";
        String borderColor = currentTheme == Theme.DARK ? "#666666" : "#ced4da";
        String textColor = getPrimaryText();
        return "-fx-background-color: " + bgColor + 
               "; -fx-padding: 8 12; -fx-border-color: " + borderColor + 
               "; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4; " +
               "-fx-alignment: center; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";";
    }
    
    /**
     * Get CSS style for ComboBox
     */
    public String getComboBoxStyle() {
        String baseColor = currentTheme == Theme.DARK ? "#20c997" : "#28a745";
        return "-fx-background-color: " + baseColor + "; -fx-text-fill: white; " +
               "-fx-border-radius: 4; -fx-background-radius: 4;";
    }
    
    /**
     * Get CSS style for Variables text area with high contrast
     */
    public String getVariablesAreaStyle() {
        // Always use white background with black text for all themes 
        // so users can clearly see their input regardless of theme
        String bgColor = "#ffffff"; // White background for all themes
        String textColor = "#000000"; // Black text for all themes
        
        return "-fx-background-color: " + bgColor + "; -fx-padding: 10; -fx-text-fill: " + textColor + ";";
    }
    
    /**
     * Get CSS style for TreeTableView with proper text visibility
     */
    public String getTreeTableViewStyle() {
        String bgColor = getSecondaryBg();
        
        // For dark theme, force ALL text to be white with comprehensive CSS
        if (currentTheme == Theme.DARK) {
            return "-fx-background-color: " + bgColor + "; " +
                   "-fx-text-fill: white; " +
                   "-fx-control-inner-background: " + bgColor + "; " +
                   "-fx-table-cell-border-color: #555555; " +
                   "-fx-selection-bar: #555555; " +
                   "-fx-selection-bar-text: white; " +
                   "-fx-focused-text-base-color: white; " +
                   "-fx-focused-mark-color: white;";
        }
        
        // For light themes, use normal colors
        String textColor = getPrimaryText();
        return "-fx-background-color: " + bgColor + "; " +
               "-fx-text-fill: " + textColor + "; " +
               "-fx-selection-bar: " + getPrimaryText() + "; " +
               "-fx-selection-bar-text: " + getSecondaryBg() + ";";
    }
}