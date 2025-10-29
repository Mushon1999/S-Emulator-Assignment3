package se.emulator.engine;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a parsed S‑program.  A program consists of a sequence of
 * instructions, a mapping of labels to instruction indices and some
 * metadata such as the program name and maximum indices of labels and
 * work variables.  A program may also contain functions that can be
 * called from the main program.  The program object is immutable once created.
 */
public final class Program {
    private final String name;
    private final List<Instruction> instructions;
    private final Map<String, Integer> labelMap;
    private final int maxLabelIndex;
    private final int maxWorkVarIndex;
    private final List<String> inputVariables;
    private final List<Function> functions;

    public Program(String name, List<Instruction> instructions, Map<String, Integer> labelMap,
                   int maxLabelIndex, int maxWorkVarIndex, List<String> inputVariables) {
        this(name, instructions, labelMap, maxLabelIndex, maxWorkVarIndex, inputVariables, Collections.emptyList());
    }

    public Program(String name, List<Instruction> instructions, Map<String, Integer> labelMap,
                   int maxLabelIndex, int maxWorkVarIndex, List<String> inputVariables, List<Function> functions) {
        this.name = name;
        this.instructions = Collections.unmodifiableList(instructions);
        this.labelMap = Collections.unmodifiableMap(labelMap);
        this.maxLabelIndex = maxLabelIndex;
        this.maxWorkVarIndex = maxWorkVarIndex;
        this.inputVariables = Collections.unmodifiableList(inputVariables);
        this.functions = Collections.unmodifiableList(functions);
    }

    public String getName() {
        return name;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    /**
     * Returns a mapping from label strings to the index of the first
     * instruction (0‑based) that bears that label.  If a program defines
     * multiple instructions with the same label, the first occurrence is
     * used as the target of jumps.
     */
    public Map<String, Integer> getLabelMap() {
        return labelMap;
    }

    /**
     * Returns the highest numeric part of any label defined in the program.
     */
    public int getMaxLabelIndex() {
        return maxLabelIndex;
    }

    /**
     * Returns the highest index of any work variable referenced in the
     * program.  New work variables created during expansion should use
     * indices strictly greater than this value to avoid collisions.
     */
    public int getMaxWorkVarIndex() {
        return maxWorkVarIndex;
    }

    /**
     * Returns an ordered list of all input variables (x1,x2,...) that
     * appear in the program.  The variables are sorted by their
     * numerical index.
     */
    public List<String> getInputVariables() {
        return inputVariables;
    }

    /**
     * Returns an ordered list of all functions defined in the program.
     * The list is immutable and ordered as they appear in the XML file.
     */
    public List<Function> getFunctions() {
        return functions;
    }
}