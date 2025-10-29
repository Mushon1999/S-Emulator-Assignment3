package se.emulator.server;

/**
 * Represents information about a function uploaded to the system.
 */
public class FunctionInfo {
    private final String name;
    private final String userString;
    private final String programName;
    private final String uploadedBy;
    private final int instructionCount;
    private final int maxExecutionLevel;
    private final long uploadTime;
    
    public FunctionInfo(String name, String userString, String programName, String uploadedBy, 
                       int instructionCount, int maxExecutionLevel) {
        this.name = name;
        this.userString = userString;
        this.programName = programName;
        this.uploadedBy = uploadedBy;
        this.instructionCount = instructionCount;
        this.maxExecutionLevel = maxExecutionLevel;
        this.uploadTime = System.currentTimeMillis();
    }
    
    // Getters
    public String getName() { return name; }
    public String getUserString() { return userString; }
    public String getProgramName() { return programName; }
    public String getUploadedBy() { return uploadedBy; }
    public int getInstructionCount() { return instructionCount; }
    public int getMaxExecutionLevel() { return maxExecutionLevel; }
    public long getUploadTime() { return uploadTime; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FunctionInfo that = (FunctionInfo) obj;
        return name.equals(that.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        return "FunctionInfo{" +
                "name='" + name + '\'' +
                ", userString='" + userString + '\'' +
                ", programName='" + programName + '\'' +
                ", uploadedBy='" + uploadedBy + '\'' +
                ", instructionCount=" + instructionCount +
                ", maxExecutionLevel=" + maxExecutionLevel +
                '}';
    }
}
