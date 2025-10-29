package se.emulator.engine;

/**
 * Base class for all S‑program instructions.  Each instruction has an
 * optional label, a line index and a static cost.  Derived classes
 * represent either a basic instruction (direct machine operation) or
 * a synthetic instruction (higher‑level macro).  Cost values are
 * accounted during program execution and expansion displays.
 */
public abstract class Instruction {
    private final String label;
    private final int index;
    private final long cost;

    protected Instruction(String label, int index, long cost) {
        this.label = label;
        this.index = index;
        this.cost = cost;
    }

    /**
     * Returns the optional label associated with this instruction.  May be
     * {@code null} if the instruction has no label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the 1‑based position of this instruction in the source file.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the precomputed static cost of this instruction.  For
     * synthetic instructions this cost reflects a conservative estimate of
     * the number of primitive operations required to implement the
     * instruction, ignoring loop iterations.  For basic instructions the
     * cost corresponds to the cycle count defined by the specification.
     */
    public long getCost() {
        return cost;
    }

    /**
     * Returns {@code true} if this instruction is one of the four
     * primitive operations defined by the S‑language.
     */
    public abstract boolean isBasic();
}