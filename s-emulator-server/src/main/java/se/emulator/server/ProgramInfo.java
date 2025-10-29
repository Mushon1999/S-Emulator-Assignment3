package se.emulator.server;

/**
 * Represents information about a program in the S-Emulator system.
 */
public class ProgramInfo {
    private final String name;
    private final String uploader;
    private final int instructionCount;
    private final int maxExecutionLevel;
    private int executionCount;
    private double averageCost;
    
    public ProgramInfo(String name, String uploader, int instructionCount, int maxExecutionLevel) {
        this.name = name;
        this.uploader = uploader;
        this.instructionCount = instructionCount;
        this.maxExecutionLevel = maxExecutionLevel;
        this.executionCount = 0;
        this.averageCost = 0.0;
    }
    
    public String getName() {
        return name;
    }
    
    public String getUploader() {
        return uploader;
    }
    
    public int getInstructionCount() {
        return instructionCount;
    }
    
    public int getMaxExecutionLevel() {
        return maxExecutionLevel;
    }
    
    public int getExecutionCount() {
        return executionCount;
    }
    
    public void incrementExecutionCount() {
        this.executionCount++;
    }
    
    public double getAverageCost() {
        return averageCost;
    }
    
    public void updateAverageCost(int cost) {
        this.averageCost = (this.averageCost * (executionCount - 1) + cost) / executionCount;
    }
}