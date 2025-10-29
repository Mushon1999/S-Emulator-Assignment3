package se.emulator.engine;

import java.util.Collections;
import java.util.List;

/**
 * Records the details of a single program run for history display.  Each
 * entry stores an incremental run number (starting at 1), the
 * expansion depth, the list of input values supplied, the resulting
 * output value y and the total cycle count.
 */
public final class HistoryEntry {
    private final int runNumber;
    private final int depth;
    private final List<Double> inputs;
    private final double yValue;
    private final long cycles;

    public HistoryEntry(int runNumber, int depth, List<Double> inputs, double yValue, long cycles) {
        this.runNumber = runNumber;
        this.depth = depth;
        this.inputs = inputs == null ? Collections.emptyList() : Collections.unmodifiableList(inputs);
        this.yValue = yValue;
        this.cycles = cycles;
    }

    public int getRunNumber() {
        return runNumber;
    }

    public int getDepth() {
        return depth;
    }

    /**
     * Alias for {@link #getDepth()}.  Returns the expansion depth used
     * during this run.  Provided for clarity when printing history.
     *
     * @return the expansion depth
     */
    public int getExpansionDepth() {
        return depth;
    }

    public List<Double> getInputs() {
        return inputs;
    }

    public double getyValue() {
        return yValue;
    }

    /**
     * Returns the result value of the output variable y for this run.
     *
     * @return the y value
     */
    public double getY() {
        return yValue;
    }

    public long getCycles() {
        return cycles;
    }
}