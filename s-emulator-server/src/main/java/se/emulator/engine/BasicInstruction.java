package se.emulator.engine;

/**
 * Represents one of the four primitive instructions of the S‑language.
 * A basic instruction can increment or decrement a variable, perform a
 * conditional jump based on a variable value, or perform a no‑op.
 */
public final class BasicInstruction extends Instruction {
    /**
     * Enumerates the supported primitive operations.
     */
    public enum Op {
        /** Assigns V ← V + 1. */
        INCREASE,
        /** Assigns V ← V - 1 (with saturation at zero). */
        DECREASE,
        /** Performs IF V ≠ 0 GOTO L. */
        JUMP_NOT_ZERO,
        /** Assigns V ← V (no operation). */
        NEUTRAL
    }

    private final Op op;
    private final VariableRef variable;
    private final String jumpLabel; // Only used for JUMP_NOT_ZERO

    /**
     * Constructs a basic instruction instance.  The cost is determined
     * automatically based on the operation type: INCREASE, DECREASE and
     * NEUTRAL cost 1 cycle; JUMP_NOT_ZERO costs 2 cycles.
     *
     * @param label optional label (may be {@code null})
     * @param index 1‑based line number
     * @param op the operation
     * @param variable the target variable
     * @param jumpLabel the target label for jumps (null for non jumps)
     */
    public BasicInstruction(String label, int index, Op op, VariableRef variable, String jumpLabel) {
        super(label, index, computeCost(op));
        this.op = op;
        this.variable = variable;
        this.jumpLabel = jumpLabel;
    }

    private static long computeCost(Op op) {
        switch (op) {
            case JUMP_NOT_ZERO:
                return 2;
            case INCREASE:
            case DECREASE:
            case NEUTRAL:
                return 1;
            default:
                return 1;
        }
    }

    public Op getOp() {
        return op;
    }

    public VariableRef getVariable() {
        return variable;
    }

    public String getJumpLabel() {
        return jumpLabel;
    }

    @Override
    public boolean isBasic() {
        return true;
    }


    public int getCycles() {
        return Math.toIntExact(getCost());
    }

}
