package se.emulator.server;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents execution history for a user.
 */
public class ExecutionHistory {
    private final String username;
    private final List<ExecutionEntry> executions;
    
    public ExecutionHistory(String username) {
        this.username = username;
        this.executions = new ArrayList<>();
    }
    
    public String getUsername() {
        return username;
    }
    
    public List<ExecutionEntry> getExecutions() {
        return new ArrayList<>(executions);
    }
    
    public void addExecution(String programName, int executionLevel, double yValue, long cycles, int cost) {
        ExecutionEntry entry = new ExecutionEntry(
            executions.size() + 1,
            programName,
            executionLevel,
            yValue,
            cycles,
            cost
        );
        executions.add(entry);
    }
    
    public static class ExecutionEntry {
        private final int runNumber;
        private final String programName;
        private final int executionLevel;
        private final double yValue;
        private final long cycles;
        private final int cost;
        
        public ExecutionEntry(int runNumber, String programName, int executionLevel, 
                             double yValue, long cycles, int cost) {
            this.runNumber = runNumber;
            this.programName = programName;
            this.executionLevel = executionLevel;
            this.yValue = yValue;
            this.cycles = cycles;
            this.cost = cost;
        }
        
        public int getRunNumber() {
            return runNumber;
        }
        
        public String getProgramName() {
            return programName;
        }
        
        public int getExecutionLevel() {
            return executionLevel;
        }
        
        public double getYValue() {
            return yValue;
        }
        
        public long getCycles() {
            return cycles;
        }
        
        public int getCost() {
            return cost;
        }
    }
}