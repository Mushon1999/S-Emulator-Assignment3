package se.emulator.engine;

import java.util.Collections;
import java.util.Map;

/**
 * Encapsulates the result of running a program.  Contains the value of
 * the output variable y, a snapshot of all variables touched during
 * execution and the total number of cycles consumed.  The variable
 * map is immutable.
 */
public final class RunResult {
    private final double yValue;
    private final Map<String, Double> variables;
    private final long cycles;

    public RunResult(double yValue, Map<String, Double> variables, long cycles) {
        this.yValue = yValue;
        this.variables = Collections.unmodifiableMap(variables);
        this.cycles = cycles;
    }

    public double getyValue() {
        return yValue;
    }

    /**
     * Returns the result value of the output variable y.  Provided for
     * consistency with getter naming conventions.
     *
     * @return the y value
     */
    public double getY() {
        return yValue;
    }

    public Map<String, Double> getVariables() {
        return variables;
    }

    public long getCycles() {
        return cycles;
    }
}