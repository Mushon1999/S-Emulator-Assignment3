package se.emulator.engine;

import java.util.Collections;
import java.util.List;

/**
 * Represents a function defined in an S-program.  A function has a name
 * (used internally), a user-string (displayed to users), and a sequence
 * of instructions that implement the function.
 */
public final class Function {
    private final String name;
    private final String userString;
    private final List<Instruction> instructions;

    public Function(String name, String userString, List<Instruction> instructions) {
        this.name = name;
        this.userString = userString;
        this.instructions = Collections.unmodifiableList(instructions);
    }

    /**
     * Returns the internal name of the function (e.g., "Successor").
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the user-visible string of the function (e.g., "S").
     * This is what should be displayed in the UI.
     */
    public String getUserString() {
        return userString;
    }

    /**
     * Returns the list of instructions that implement this function.
     */
    public List<Instruction> getInstructions() {
        return instructions;
    }
}