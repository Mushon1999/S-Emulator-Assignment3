package se.emulator.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a high‑level synthetic instruction.  Synthetic instructions
 * correspond to so‑called "synthetic sugars" defined for the S
 * language.  Each synthetic instruction expands into a sequence of
 * primitive instructions; nevertheless during execution at depth 0
 * synthetic instructions are treated atomically and consume a fixed
 * number of cycles.  The cost estimates used here are conservative
 * approximations of the number of primitive operations required.
 */
public final class SyntheticInstruction extends Instruction {
    /**
     * Supported synthetic operations.
     */
    public enum Op {
    /** Assigns V ← 0. */
    ZERO_VARIABLE,
    /** Assigns Vdest ← Vsrc. */
    ASSIGNMENT,
    /** Assigns V ← constant. */
    CONSTANT_ASSIGNMENT,
    /** Performs an unconditional jump to a label. */
    GOTO_LABEL,
    /** Performs IF V = 0 GOTO L. */
    JUMP_ZERO,
    /** Performs IF V = constant GOTO L. */
    JUMP_EQUAL_CONSTANT,
    /** Performs IF V1 = V2 GOTO L. */
    JUMP_EQUAL_VARIABLE,
    /** Calls a function with arguments (function composition). */
    QUOTE,
    /** Prompts for user input for a variable. */
    INPUT
    }

    private final Op op;
    private final VariableRef variable;
    private final Map<String, String> args;

    /**
     * Constructs a synthetic instruction.  The cost is computed using a
     * static model that counts the primitive operations required to
     * implement the high‑level behaviour, ignoring any loop iteration
     * counts that may arise during runtime.
     *
     * @param label optional label
     * @param index line number (1‑based)
     * @param op operation
     * @param variable primary variable for this instruction
     * @param args map of argument names to values (may be {@code null})
     */
    public SyntheticInstruction(String label, int index, Op op, VariableRef variable, Map<String, String> args) {
        super(label, index, staticCost(op, args));
        this.op = op;
        this.variable = variable;
        this.args = args == null ? Collections.emptyMap() : new HashMap<>(args);
    }

    public Op getOp() {
        return op;
    }

    public VariableRef getVariable() {
        return variable;
    }

    public Map<String, String> getArgs() {
        return Collections.unmodifiableMap(args);
    }

    @Override
    public boolean isBasic() {
        return false;
    }

    /**
     * Computes a conservative static cost estimate for the synthetic
     * instruction.  These values are based on the expansion strategies
     * implemented in {@link se.emulator.engine.EngineServiceImpl}.  They
     * ignore the runtime values of variables and thus produce a fixed
     * cycle count for each operation type.
     */
    private static long staticCost(Op op, Map<String, String> args) {
    switch (op) {
            case ZERO_VARIABLE:
                // Implemented via copy from a fresh zero variable using the
                // assignment algorithm.  The copy algorithm allocates a
                // temporary and loops through three phases, totalling 17 cycles.
                return 17;
            case ASSIGNMENT:
                // Copying from another variable uses the copy algorithm once.
                return 17;
            case CONSTANT_ASSIGNMENT: {
                long c = 0;
                if (args != null) {
                    String val = args.get("constantValue");
                    if (val != null) {
                        try {
                            c = Long.parseLong(val);
                        } catch (NumberFormatException ignored) {
                            c = 0;
                        }
                    }
                }
                // ZERO dest (17 cycles) + c increments and a neutral (1).
                return 17 + c + 1;
            }
            case GOTO_LABEL:
                // Unconditional goto uses: INCREASE temp + JNZ temp; plus a neutral.
                return 3;
            case JUMP_ZERO:
                // Implemented via: JNZ v skip + unconditional goto + neutral.
                // JNZ (2) + INCREASE (1) + JNZ (2) + NEUTRAL (1) = 6.
                return 6;
            case JUMP_EQUAL_CONSTANT: {
                long c2 = 0;
                if (args != null) {
                    String val = args.get("constantValue");
                    if (val != null) {
                        try {
                            c2 = Long.parseLong(val);
                        } catch (NumberFormatException ignored) {
                            c2 = 0;
                        }
                    }
                }
                // Copy v (17) + subtract constant (c2) + JNZ (2) + unconditional (3) + NEUTRAL (1).
                return 17 + c2 + 2 + 3 + 1;
            }
            case JUMP_EQUAL_VARIABLE:
                // Copy v and u (2 × 17) + comparison scaffold (~15) + unconditional (3) + neutrals.
                // Use 49 as a conservative estimate.
                return 49;
            case INPUT:
                // INPUT is a no-op for cost (user input handled in UI)
                return 1;
            default:
                return 1;
        }
    }
    public int getCycles() {
        return Math.toIntExact(getCost());
    }

}