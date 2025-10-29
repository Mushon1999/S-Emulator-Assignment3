package se.emulator.server;

/**
 * Represents information about a function in the S-Emulator system.
 */
public class FunctionInfo {
    private final String name;
    private final String userString;
    private final String parentProgram;
    private final String uploader;
    private final int instructionCount;
    private final int maxExecutionLevel;
    
    public FunctionInfo(String name, String userString, String parentProgram, 
                       String uploader, int instructionCount, int maxExecutionLevel) {
        this.name = name;
        this.userString = userString;
        this.parentProgram = parentProgram;
        this.uploader = uploader;
        this.instructionCount = instructionCount;
        this.maxExecutionLevel = maxExecutionLevel;
    }
    
    public String getName() {
        return name;
    }
    
    public String getUserString() {
        return userString;
    }
    
    public String getParentProgram() {
        return parentProgram;
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
}