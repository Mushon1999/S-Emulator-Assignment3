package se.emulator.server;

/**
 * Represents an execution history entry for a user.
 */
public class ExecutionHistory {
    private final int runNumber;
    private final boolean isMainProgram;
    private final String programOrFunctionName;
    private final String architectureType;
    private final int executionLevel;
    private final long finalYValue;
    private final int cyclesConsumed;
    private final long creditsSpent;
    private final long executionTime;
    
    public ExecutionHistory(int runNumber, boolean isMainProgram, String programOrFunctionName,
                           String architectureType, int executionLevel, long finalYValue,
                           int cyclesConsumed, long creditsSpent) {
        this.runNumber = runNumber;
        this.isMainProgram = isMainProgram;
        this.programOrFunctionName = programOrFunctionName;
        this.architectureType = architectureType;
        this.executionLevel = executionLevel;
        this.finalYValue = finalYValue;
        this.cyclesConsumed = cyclesConsumed;
        this.creditsSpent = creditsSpent;
        this.executionTime = System.currentTimeMillis();
    }
    
    // Getters
    public int getRunNumber() { return runNumber; }
    public boolean isMainProgram() { return isMainProgram; }
    public String getProgramOrFunctionName() { return programOrFunctionName; }
    public String getArchitectureType() { return architectureType; }
    public int getExecutionLevel() { return executionLevel; }
    public long getFinalYValue() { return finalYValue; }
    public int getCyclesConsumed() { return cyclesConsumed; }
    public long getCreditsSpent() { return creditsSpent; }
    public long getExecutionTime() { return executionTime; }
    
    @Override
    public String toString() {
        return "ExecutionHistory{" +
                "runNumber=" + runNumber +
                ", isMainProgram=" + isMainProgram +
                ", programOrFunctionName='" + programOrFunctionName + '\'' +
                ", architectureType='" + architectureType + '\'' +
                ", executionLevel=" + executionLevel +
                ", finalYValue=" + finalYValue +
                ", cyclesConsumed=" + cyclesConsumed +
                ", creditsSpent=" + creditsSpent +
                '}';
    }
}
