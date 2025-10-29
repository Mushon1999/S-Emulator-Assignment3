package se.emulator.server;

/**
 * Represents a user in the S-Emulator system.
 */
public class User {
    private final String username;
    private long credits;
    private int mainPrograms;
    private int helperFunctions;
    private long totalCreditsUsed;
    private int executionCount;
    
    public User(String username, long initialCredits) {
        this.username = username;
        this.credits = initialCredits;
        this.mainPrograms = 0;
        this.helperFunctions = 0;
        this.totalCreditsUsed = 0;
        this.executionCount = 0;
    }
    
    public String getUsername() {
        return username;
    }
    
    public long getCredits() {
        return credits;
    }
    
    public void deductCredits(long amount) {
        this.credits -= amount;
        this.totalCreditsUsed += amount;
    }
    
    public void addCredits(long amount) {
        this.credits += amount;
    }
    
    public int getMainPrograms() {
        return mainPrograms;
    }
    
    public void incrementMainPrograms() {
        this.mainPrograms++;
    }
    
    public int getHelperFunctions() {
        return helperFunctions;
    }
    
    public void incrementHelperFunctions() {
        this.helperFunctions++;
    }
    
    public long getTotalCreditsUsed() {
        return totalCreditsUsed;
    }
    
    public int getExecutionCount() {
        return executionCount;
    }
    
    public void incrementExecutionCount() {
        this.executionCount++;
    }
}