package se.emulator.server;

/**
 * Represents the result of a program execution.
 */
public class ExecutionResult {
    private final boolean success;
    private final String message;
    private final double yValue;
    private final long cycles;
    private final int cost;
    
    public ExecutionResult(boolean success, String message, double yValue, long cycles, int cost) {
        this.success = success;
        this.message = message;
        this.yValue = yValue;
        this.cycles = cycles;
        this.cost = cost;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
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
