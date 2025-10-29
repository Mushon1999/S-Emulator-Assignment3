package se.emulator.engine;

import java.util.List;

/**
 * Facade for interacting with an {@link se.emulator.engine.Program}.  The UI
 * layer uses this interface to load programs, display them, perform
 * expansions, execute runs and inspect execution history.  The engine
 * layer is intentionally unaware of the environment in which it runs
 * (console, GUI or server) and thus exposes only pure methods.
 */
public interface EngineService {
    /**
     * Attempts to load a program from the given file path.  If the
     * returned {@link LoadResult#isSuccess()} is {@code true} then the
     * program has been loaded and future calls to other methods will act
     * on this program.  Loading a new program replaces any previously
     * loaded program and clears execution history.
     *
     * @param filePath a filesystem path to an XML program definition
     * @return result indicating success or failure and an informative
     * message
     */
    LoadResult loadSystemFile(String filePath);

    /**
     * Produces a textual representation of the currently loaded program.
     * If no program is loaded this method returns an explanatory
     * message.  The format is described in the assignment specification.
     */
    String displayProgram();

    /**
     * Returns the maximum expansion depth currently supported for the
     * loaded program.  Depth 0 is always valid (no expansion).  Depth 1
     * exists if at least one synthetic instruction is present.
     */
    int getMaxExpansionDepth();

    /**
     * Expands the currently loaded program to the given depth and
     * returns a textual representation of the expanded program.  Depth
     * values beyond the maximum are capped.  Depth 0 returns the same
     * output as {@link #displayProgram()}.
     */
    String expandProgram(int depth);

    /**
     * Executes the currently loaded program with the given inputs and
     * expansion depth.  Inputs are mapped to variables x1,x2,… in the
     * order provided; missing inputs default to 0.  Execution always
     * honours the semantics of synthetic instructions regardless of the
     * depth.  The depth affects only the representation returned via
     * {@link #expandProgram(int)}; semantics remain identical.
     *
     * @param depth requested expansion depth
     * @param inputs list of input values to map to x1,x2,… (null or
     *               shorter lists imply zeros for missing inputs)
     * @return a result object containing y, variable snapshot and cycle
     *         count, or {@code null} if no program is loaded
     */
    RunResult runProgram(int depth, List<Double> inputs);

    /**
     * Returns a snapshot of all recorded run history.  The list is
     * immutable and ordered from oldest to newest.  If no runs have
     * occurred an empty list is returned.
     */
    List<HistoryEntry> getHistory();

    /**
     * Returns an ordered list of input variable names (x1,x2,…) that appear
     * in the currently loaded program.  The names are sorted by their
     * numeric index.  If no program is loaded an empty list is returned.
     *
     * @return list of input variable names used in the program
     */
    List<String> getInputVariables();

    /**
     * Returns the name of the currently loaded program.  If no program
     * is loaded returns null.
     *
     * @return the program name or null if no program is loaded
     */
    String getProgramName();

    /**
     * Returns an ordered list of all functions defined in the currently
     * loaded program.  The list contains Function objects with name,
     * user-string, and instructions.  If no program is loaded an empty
     * list is returned.
     *
     * @return list of functions defined in the program
     */
    List<Function> getFunctions();

    /**
     * Returns the list of instructions in the currently loaded program
     * at the specified expansion depth. This is useful for debugging
     * and step-by-step execution.
     *
     * @param depth expansion depth for synthetic instructions
     * @return list of instructions at the specified depth
     */
    List<Instruction> getInstructions(int depth);

    /**
     * Initializes a debug session for step-by-step execution.
     * Returns a debug context that can be used for stepping.
     *
     * @param depth expansion depth
     * @param inputs input values for execution
     * @return debug context or null if no program is loaded
     */
    DebugContext initializeDebugSession(int depth, List<Double> inputs);

    /**
     * Executes a single instruction step in the debug session.
     * Returns the updated debug context after the step.
     *
     * @param context current debug context
     * @return updated context after step execution
     */
    DebugContext executeDebugStep(DebugContext context);
    
    /**
     * Steps backward in the debug session if possible.
     * Returns true if step back was successful, false otherwise.
     *
     * @param context current debug context
     * @return true if stepped back successfully
     */
    boolean stepBackward(DebugContext context);
    
}