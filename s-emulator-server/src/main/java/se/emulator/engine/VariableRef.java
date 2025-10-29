package se.emulator.engine;

/**
 * Represents a reference to a program variable.  The S‑language has three
 * families of variables: input variables (x1,x2,…), work variables
 * (z1,z2,…), and the single output variable y.  This class encapsulates
 * the parsed name, its family and (where relevant) its numerical index.
 */
public final class VariableRef {
    /**
     * Enumeration of supported variable families.
     */
    public enum Type {
        /** Input variables, named x1,x2,… */
        X,
        /** Work variables, named z1,z2,… */
        Z,
        /** Output variable, named y */
        Y
    }

    private final Type type;
    private final int index; // Only meaningful for X and Z families.
    private final String name;

    private VariableRef(Type type, int index, String name) {
        this.type = type;
        this.index = index;
        this.name = name;
    }

    /**
     * Parses a textual variable name into a {@link VariableRef}.  The
     * supported patterns are "y" for the output variable, "xn" for input
     * variable n (1‑based) and "zn" for work variable n (1‑based).  Names
     * are case insensitive.  A leading/trailing whitespace is ignored.
     *
     * @param text the textual name
     * @return a parsed variable reference
     * @throws IllegalArgumentException if the name is not a valid S
     *                                  variable identifier
     */
    public static VariableRef fromString(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Variable name must not be null");
        }
        String trimmed = text.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Variable name must not be empty");
        }
        if ("y".equals(trimmed)) {
            return new VariableRef(Type.Y, 0, "y");
        }
        char first = trimmed.charAt(0);
        if (first == 'x' || first == 'z') {
            String numPart = trimmed.substring(1);
            if (numPart.isEmpty()) {
                throw new IllegalArgumentException("Variable name '" + text + "' is missing an index");
            }
            int idx;
            try {
                idx = Integer.parseInt(numPart);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Variable name '" + text + "' has an invalid index");
            }
            if (idx <= 0) {
                throw new IllegalArgumentException("Variable name '" + text + "' must have a positive index");
            }
            Type type = (first == 'x') ? Type.X : Type.Z;
            return new VariableRef(type, idx, first + numPart);
        }
        throw new IllegalArgumentException("Variable name '" + text + "' is not recognised");
    }

    /**
     * Returns the family of this variable.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the 1‑based index of this variable.  For the y variable the
     * index is always 0.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the canonical lowercase name of this variable (e.g. x1, z3,
     * y).
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}