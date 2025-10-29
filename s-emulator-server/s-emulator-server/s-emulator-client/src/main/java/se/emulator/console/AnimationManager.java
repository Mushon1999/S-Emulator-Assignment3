package se.emulator.console;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.*;
import java.util.Properties;

/**
 * AnimationManager handles startup animations for the S-Emulator application.
 * Provides 3 different animation types with user configurable enable/disable functionality.
 */
public class AnimationManager {
    
    public enum AnimationType {
        FADE_IN,
        SLIDE_IN,
        SCALE_UP
    }
    
    private static final String SETTINGS_FILE = "animation_settings.properties";
    private static final Duration ANIMATION_DURATION = Duration.seconds(1.5); // Max 2 seconds as requested
    
    private boolean animationsEnabled = true;
    private AnimationType selectedAnimation = AnimationType.FADE_IN;
    
    public AnimationManager() {
        loadSettings();
    }
    
    /**
     * Play the selected startup animation on the main stage
     */
    public void playStartupAnimation(Stage stage, Scene scene, Runnable onComplete) {
        if (!animationsEnabled) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        
        Node root = scene.getRoot();
        
        switch (selectedAnimation) {
            case FADE_IN:
                playFadeInAnimation(root, onComplete);
                break;
            case SLIDE_IN:
                playSlideInAnimation(root, onComplete);
                break;
            case SCALE_UP:
                playScaleUpAnimation(root, onComplete);
                break;
        }
    }
    
    /**
     * Fade-in animation: Window fades from transparent to opaque
     */
    private void playFadeInAnimation(Node root, Runnable onComplete) {
        root.setOpacity(0.0);
        
        FadeTransition fadeIn = new FadeTransition(ANIMATION_DURATION, root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(Interpolator.EASE_OUT);
        
        if (onComplete != null) {
            fadeIn.setOnFinished(e -> onComplete.run());
        }
        
        fadeIn.play();
    }
    
    /**
     * Slide-in animation: Window slides in from the top
     */
    private void playSlideInAnimation(Node root, Runnable onComplete) {
        double originalY = root.getTranslateY();
        root.setTranslateY(-600); // Start above screen
        root.setOpacity(0.8);
        
        TranslateTransition slideIn = new TranslateTransition(ANIMATION_DURATION, root);
        slideIn.setFromY(-600);
        slideIn.setToY(originalY);
        slideIn.setInterpolator(Interpolator.EASE_OUT);
        
        FadeTransition fadeIn = new FadeTransition(ANIMATION_DURATION, root);
        fadeIn.setFromValue(0.8);
        fadeIn.setToValue(1.0);
        
        ParallelTransition parallelAnimation = new ParallelTransition(slideIn, fadeIn);
        
        if (onComplete != null) {
            parallelAnimation.setOnFinished(e -> onComplete.run());
        }
        
        parallelAnimation.play();
    }
    
    /**
     * Scale-up animation: Window scales from small to normal size
     */
    private void playScaleUpAnimation(Node root, Runnable onComplete) {
        root.setScaleX(0.3);
        root.setScaleY(0.3);
        root.setOpacity(0.5);
        
        ScaleTransition scaleUp = new ScaleTransition(ANIMATION_DURATION, root);
        scaleUp.setFromX(0.3);
        scaleUp.setFromY(0.3);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);
        
        FadeTransition fadeIn = new FadeTransition(ANIMATION_DURATION, root);
        fadeIn.setFromValue(0.5);
        fadeIn.setToValue(1.0);
        
        ParallelTransition parallelAnimation = new ParallelTransition(scaleUp, fadeIn);
        
        if (onComplete != null) {
            parallelAnimation.setOnFinished(e -> onComplete.run());
        }
        
        parallelAnimation.play();
    }
    
    /**
     * Enable or disable startup animations
     */
    public void setAnimationsEnabled(boolean enabled) {
        this.animationsEnabled = enabled;
        saveSettings();
    }
    
    /**
     * Check if animations are currently enabled
     */
    public boolean areAnimationsEnabled() {
        return animationsEnabled;
    }
    
    /**
     * Set the animation type to use
     */
    public void setAnimationType(AnimationType type) {
        this.selectedAnimation = type;
        saveSettings();
    }
    
    /**
     * Get the current animation type
     */
    public AnimationType getAnimationType() {
        return selectedAnimation;
    }
    
    /**
     * Get all available animation types for UI selection
     */
    public AnimationType[] getAvailableAnimations() {
        return AnimationType.values();
    }
    
    /**
     * Get display name for animation type
     */
    public String getAnimationDisplayName(AnimationType type) {
        switch (type) {
            case FADE_IN: return "Fade In";
            case SLIDE_IN: return "Slide In";
            case SCALE_UP: return "Scale Up";
            default: return type.name();
        }
    }
    
    /**
     * Save animation settings to file
     */
    private void saveSettings() {
        Properties props = new Properties();
        props.setProperty("animations.enabled", String.valueOf(animationsEnabled));
        props.setProperty("animations.type", selectedAnimation.name());
        
        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            props.store(fos, "S-Emulator Animation Settings");
        } catch (IOException e) {
            System.err.println("Could not save animation settings: " + e.getMessage());
        }
    }
    
    /**
     * Load animation settings from file
     */
    private void loadSettings() {
        Properties props = new Properties();
        
        try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
            props.load(fis);
            
            String enabledStr = props.getProperty("animations.enabled", "true");
            animationsEnabled = Boolean.parseBoolean(enabledStr);
            
            String typeStr = props.getProperty("animations.type", "FADE_IN");
            try {
                selectedAnimation = AnimationType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                selectedAnimation = AnimationType.FADE_IN; // Default fallback
            }
            
        } catch (IOException e) {
            // File doesn't exist or can't be read - use defaults
            animationsEnabled = true;
            selectedAnimation = AnimationType.FADE_IN;
        }
    }
}