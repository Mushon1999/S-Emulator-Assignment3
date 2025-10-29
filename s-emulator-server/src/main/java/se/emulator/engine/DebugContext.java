package se.emulator.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Debug context for step-by-step execution of S-Emulator programs.
 * Contains the current execution state including program counter,
 * variable values, and execution metadata. Supports step backward through
 * state history tracking.
 */
public class DebugContext {
    private final List<Instruction> instructions;
    private final List<Double> inputs;
    private final Map<String, Double> variables;
    private int programCounter;
    private int cycles;
    private boolean finished;
    private Double result;
    private String lastExecutedInstruction;
    private int depth;
    
    // State history for backward stepping
    private final List<DebugState> stateHistory;
    
    /**
     * Internal class to capture state snapshots for backward stepping
     */
    private static class DebugState {
        final Map<String, Double> variables;
        final int programCounter;
        final int cycles;
        final boolean finished;
        final Double result;
        final String lastExecutedInstruction;
        
        DebugState(Map<String, Double> variables, int programCounter, int cycles, 
                  boolean finished, Double result, String lastExecutedInstruction) {
            this.variables = new HashMap<>(variables);
            this.programCounter = programCounter;
            this.cycles = cycles;
            this.finished = finished;
            this.result = result;
            this.lastExecutedInstruction = lastExecutedInstruction;
        }
    }

    public DebugContext(List<Instruction> instructions, List<Double> inputs, int depth) {
        this.instructions = instructions;
        this.inputs = inputs;
        this.depth = depth;
        this.variables = new HashMap<>();
        this.programCounter = 0;
        this.cycles = 0;
        this.finished = false;
        this.result = null;
        this.lastExecutedInstruction = "";
        this.stateHistory = new ArrayList<>();
        
        // Initialize input variables
        for (int i = 0; i < inputs.size(); i++) {
            variables.put("x" + (i + 1), inputs.get(i));
        }
        
        // Save initial state
        saveCurrentState();
    }

    // Getters
    public List<Instruction> getInstructions() { return instructions; }
    public List<Double> getInputs() { return inputs; }
    public Map<String, Double> getVariables() { return new HashMap<>(variables); }
    public int getProgramCounter() { return programCounter; }
    public int getCycles() { return cycles; }
    public boolean isFinished() { return finished; }
    public Double getResult() { return result; }
    public String getLastExecutedInstruction() { return lastExecutedInstruction; }
    public int getDepth() { return depth; }

    // Setters for engine use
    public void setProgramCounter(int pc) { 
        this.programCounter = pc; 
    }
    
    public void incrementCycles() { 
        this.cycles++; 
    }
    
    public void addCycles(long cyclesToAdd) {
        this.cycles += cyclesToAdd;
    }
    
    public void setFinished(boolean finished) { 
        this.finished = finished; 
    }
    
    public void setResult(Double result) { 
        this.result = result; 
    }
    
    public void setLastExecutedInstruction(String instruction) { 
        this.lastExecutedInstruction = instruction; 
    }
    
    public void setVariable(String name, Double value) {
        variables.put(name, value);
    }
    
    public Double getVariable(String name) {
        return variables.get(name);
    }

    public Instruction getCurrentInstruction() {
        if (programCounter >= 0 && programCounter < instructions.size()) {
            return instructions.get(programCounter);
        }
        return null;
    }

    public boolean hasMoreInstructions() {
        return programCounter < instructions.size();
    }
    
    /**
     * Save current state before executing next instruction
     */
    public void saveCurrentState() {
        DebugState state = new DebugState(variables, programCounter, cycles, 
                                        finished, result, lastExecutedInstruction);
        stateHistory.add(state);
    }
    
    /**
     * Step backward to previous state if possible
     */
    public boolean stepBackward() {
        if (stateHistory.size() > 1) {
            // Remove current state and restore previous
            stateHistory.remove(stateHistory.size() - 1);
            DebugState previousState = stateHistory.get(stateHistory.size() - 1);
            
            // Restore previous state
            variables.clear();
            variables.putAll(previousState.variables);
            this.programCounter = previousState.programCounter;
            this.cycles = previousState.cycles;
            this.finished = previousState.finished;
            this.result = previousState.result;
            this.lastExecutedInstruction = previousState.lastExecutedInstruction;
            
            return true;
        }
        return false;
    }
    
    /**
     * Check if we can step backward
     */
    public boolean canStepBackward() {
        return stateHistory.size() > 1;
    }
}