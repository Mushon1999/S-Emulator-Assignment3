package se.emulator.server;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents information about a program uploaded to the system.
 */
public class ProgramInfo {
    private final String name;
    private final String uploadedBy;
    private final int instructionCount;
    private final int maxExecutionLevel;
    private final AtomicInteger executionCount;
    private final AtomicLong totalCreditsSpent;
    private final long uploadTime;
    
    public ProgramInfo(String name, String uploadedBy, int instructionCount, int maxExecutionLevel) {
        this.name = name;
        this.uploadedBy = uploadedBy;
        this.instructionCount = instructionCount;
        this.maxExecutionLevel = maxExecutionLevel;
        this.executionCount = new AtomicInteger(0);
        this.totalCreditsSpent = new AtomicLong(0);
        this.uploadTime = System.currentTimeMillis();
    }
    
    // Getters
    public String getName() { return name; }
    public String getUploadedBy() { return uploadedBy; }
    public int getInstructionCount() { return instructionCount; }
    public int getMaxExecutionLevel() { return maxExecutionLevel; }
    public int getExecutionCount() { return executionCount.get(); }
    public long getTotalCreditsSpent() { return totalCreditsSpent.get(); }
    public long getUploadTime() { return uploadTime; }
    
    // Calculated properties
    public double getAverageCreditCost() {
        int count = executionCount.get();
        return count > 0 ? (double) totalCreditsSpent.get() / count : 0.0;
    }
    
    // Statistics updates
    public void recordExecution(long creditsSpent) {
        executionCount.incrementAndGet();
        totalCreditsSpent.addAndGet(creditsSpent);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProgramInfo that = (ProgramInfo) obj;
        return name.equals(that.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        return "ProgramInfo{" +
                "name='" + name + '\'' +
                ", uploadedBy='" + uploadedBy + '\'' +
                ", instructionCount=" + instructionCount +
                ", maxExecutionLevel=" + maxExecutionLevel +
                ", executionCount=" + executionCount.get() +
                ", averageCreditCost=" + getAverageCreditCost() +
                '}';
    }
}
