package se.emulator.server;

/**
 * Represents a chat message in the S-Emulator system.
 */
public class ChatMessage {
    private final String username;
    private final String message;
    private final long timestamp;
    
    public ChatMessage(String username, String message) {
        this.username = username;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    public ChatMessage(String username, String message, long timestamp) {
        this.username = username;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getMessage() {
        return message;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}