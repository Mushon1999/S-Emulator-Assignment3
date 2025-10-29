package se.emulator.engine;
import java.util.*;
import java.util.regex.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link EngineService}.  Provides program
 * parsing, display, expansion, execution and history tracking.
 * Instances of this class are not thread safe but can be reused
 * sequentially by the console UI.
 */
public final class EngineServiceImpl implements EngineService {
    private Program currentProgram;
    private final List<HistoryEntry> history = new ArrayList<>();
    private int runCounter = 0;

    @Override
    public LoadResult loadSystemFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return new LoadResult(false, "File path must not be empty.");
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return new LoadResult(false, "File not found or not a file: " + filePath);
        }
        // enforce .xml suffix
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".xml")) {
            return new LoadResult(false, "File must have a .xml extension: " + filePath);
        }
        Program parsed;
        try {
            parsed = parseProgram(file);
        } catch (Exception ex) {
            // Use the exception message as user friendly feedback
            return new LoadResult(false, ex.getMessage());
        }
        // Program loaded successfully: reset state
        currentProgram = parsed;
        history.clear();
        runCounter = 0;
        return new LoadResult(true, "Program loaded successfully.");
    }

    @Override
    public String displayProgram() {
    if (currentProgram == null) {
        return "No program is loaded.";
    }
    StringBuilder header = new StringBuilder();
    // Program name
    header.append("Program: ").append(currentProgram.getName()).append(System.lineSeparator());
    // Inputs used: collect from service API if available, otherwise regex from rendered commands
    List<String> inputs;
    try {
        inputs = getInputVariables();
    } catch (Exception e) {
        // Fallback via regex on rendered lines
        inputs = new ArrayList<>();
    }
    if (inputs == null || inputs.isEmpty()) {
        // derive from instructions text
        Set<String> inSet = new TreeSet<>(Comparator.comparingInt(s -> Integer.parseInt(s.substring(1))));
        for (Instruction ins : currentProgram.getInstructions()) {
            String rendered = (ins instanceof SyntheticInstruction)
                    ? formatSynthetic((SyntheticInstruction) ins)
                    : formatBasic((BasicInstruction) ins);
            Matcher m = Pattern.compile("\\bx(\\d+)\\b").matcher(rendered);
            while (m.find()) {
                inSet.add("x" + m.group(1));
            }
        }
        inputs = new ArrayList<>(inSet);
    }
    header.append("Inputs: ");
    if (inputs.isEmpty()) {
        header.append("(none)");
    } else {
        header.append(String.join(", ", inputs));
    }
    header.append(System.lineSeparator());

    // Labels list in order of appearance (EXIT last if referenced)
    LinkedHashSet<String> labels = new LinkedHashSet<>();
    boolean hasExitRef = false;
    for (Instruction ins : currentProgram.getInstructions()) {
        String lbl = ins.getLabel();
        if (lbl != null && !lbl.isBlank()) {
            labels.add(lbl);
        }
        String rendered = (ins instanceof SyntheticInstruction)
                ? formatSynthetic((SyntheticInstruction) ins)
                : formatBasic((BasicInstruction) ins);
        if (rendered.toUpperCase().contains("GOTO EXIT") || rendered.toUpperCase().contains(" = EXIT") ) {
            hasExitRef = true;
        }
    }
    List<String> labelList = new ArrayList<>(labels);
    if (hasExitRef) {
        labelList.add("EXIT");
    }
    header.append("Labels: ");
    if (labelList.isEmpty()) {
        header.append("(none)");
    } else {
        header.append(String.join(", ", labelList));
    }
    header.append(System.lineSeparator());

    // Now the instructions
    StringBuilder sb = new StringBuilder();
    sb.append(header);
    List<Instruction> insList = currentProgram.getInstructions();
    for (int i = 0; i < insList.size(); i++) {
        Instruction ins = insList.get(i);
        String line = renderInstructionLine(i + 1, ins, null);
        sb.append(line).append(System.lineSeparator());
    }
    return sb.toString().trim();
}

    @Override
    public int getMaxExpansionDepth() {
        if (currentProgram == null) {
            return 0;
        }
        boolean hasSynthetic = false;
        for (Instruction ins : currentProgram.getInstructions()) {
            if (!ins.isBasic()) {
                hasSynthetic = true;
                break;
            }
        }
        return hasSynthetic ? 1 : 0;
    }

    @Override
    public String expandProgram(int depth) {
        if (currentProgram == null) {
            return "No program is loaded.";
        }
        int max = getMaxExpansionDepth();
        int d = (depth <= 0) ? 0 : depth;
        if (d > max) {
            d = max;
        }
        if (d == 0) {
            return displayProgram();
        }
        // Depth 1: expand all synthetic instructions one level
        StringBuilder sb = new StringBuilder();
        int nextLabelIndex = currentProgram.getMaxLabelIndex() + 1;
        int nextWorkVarIndex = currentProgram.getMaxWorkVarIndex() + 1;
        List<Instruction> insList = currentProgram.getInstructions();
        int lineNumber = 1;
        for (Instruction ins : insList) {
            if (ins.isBasic()) {
                BasicInstruction b = (BasicInstruction) ins;
                String type = "B";
                String labelField = formatLabelField(b.getLabel());
                String command = formatBasic(b);
                sb.append(String.format("#%d (%s) [ %s ] %s (%d)", lineNumber++, type, labelField, command, b.getCost()));
                sb.append(System.lineSeparator());
            } else {
                SyntheticInstruction syn = (SyntheticInstruction) ins;
                List<ExpandedLine> lines = expandSyntheticInstruction(syn, ins.getIndex(), syn.getLabel(), nextLabelIndex, nextWorkVarIndex);
                // update indices based on expansions
                int labelsUsed = 0;
                int workUsed = 0;
                for (ExpandedLine l : lines) {
                    labelsUsed += l.newLabelsUsed;
                    workUsed += l.newWorkVarsUsed;
                }
                nextLabelIndex += labelsUsed;
                nextWorkVarIndex += workUsed;
                for (ExpandedLine el : lines) {
                    String type = "B";
                    String labelField = formatLabelField(el.label);
                    String ancestry = "";
                    if (el.origins != null && !el.origins.isEmpty()) {
                        StringBuilder anc = new StringBuilder();
                        for (int k = 0; k < el.origins.size(); k++) {
                            anc.append("<<< #").append(el.origins.get(k));
                            if (k < el.origins.size() - 1) {
                                anc.append(' ');
                            }
                        }
                        ancestry = " " + anc.toString();
                    }
                    sb.append(String.format("#%d (%s) [ %s ] %s (%d)%s", lineNumber++, type, labelField, el.command, el.cost, ancestry));
                    sb.append(System.lineSeparator());
                }
            }
        }
        // remove trailing newline
        if (sb.length() > 0) {
            sb.setLength(sb.length() - System.lineSeparator().length());
        }
        return sb.toString();
    }

    @Override
    public RunResult runProgram(int depth, List<Double> inputs) {
        if (currentProgram == null) {
            return null;
        }
        int max = getMaxExpansionDepth();
        int d = (depth <= 0) ? 0 : depth;
        if (d > max) {
            d = max;
        }
        // Prepare variables map
        Map<String, Double> vars = new HashMap<>();
        // Map inputs to x1,x2,...
        int inputCount = (inputs == null) ? 0 : inputs.size();
        for (int i = 0; i < inputCount; i++) {
            Double valObj = inputs.get(i);
            double val = (valObj == null) ? 0.0 : valObj;
            vars.put("x" + (i + 1), val);
        }
        
        // Initialise known work variables to zero
        for (int i = 1; i <= currentProgram.getMaxWorkVarIndex(); i++) {
            String name = "z" + i;
            vars.putIfAbsent(name, 0.0);
        }
        // Initialise y
        vars.putIfAbsent("y", 0.0);
        long cycles = 0;
        List<Instruction> insList = currentProgram.getInstructions();
        Map<String, Integer> labelMap = currentProgram.getLabelMap();
        int pc = 0;
        
        // DEBUG: Program start information
        System.out.println("=== PROGRAM EXECUTION START ===");
        System.out.println("Total instructions: " + insList.size());
        System.out.println("Initial PC: " + pc);
        System.out.println("Label map: " + labelMap);
        System.out.println("Initial variables: " + vars);
        
        // DEBUG: Print all instructions
        for (int i = 0; i < Math.min(insList.size(), 10); i++) {
            Instruction ins = insList.get(i);
            String label = ins.getLabel();
            String labelStr = (label != null) ? "[" + label + "] " : "";
            System.out.println("PC=" + i + ": " + labelStr + ins);
        }
        
        System.out.println("=== BEGIN EXECUTION ===");
        
        // Add infinite loop protection
        final long MAX_CYCLES = 1000000; // Reduced back to 1M for faster testing
        long debugCycleCheck = 10000; // Print debug info every 10k cycles (more frequent)
        
        // Execution loop
        while (pc >= 0 && pc < insList.size() && cycles < MAX_CYCLES) {
            Instruction ins = insList.get(pc);
            cycles += ins.getCost();
            
            // Debug PC and instruction execution - especially early instructions
            if (pc <= 10) {
                System.out.println("DEBUG EARLY PC=" + pc + ": Executing instruction: " + ins);
                if (ins.isBasic()) {
                    BasicInstruction b = (BasicInstruction) ins;
                    System.out.println("  BASIC: var=" + b.getVariable().getName() + ", op=" + b.getOp());
                } else {
                    SyntheticInstruction s = (SyntheticInstruction) ins;
                    System.out.println("  SYNTHETIC: var=" + s.getVariable().getName() + ", op=" + s.getOp() + ", args=" + s.getArgs());
                }
            } else {
                System.out.println("PC=" + pc + ": Executing instruction: " + ins);
            }
            
            // Debug output for long-running executions
            if (cycles % debugCycleCheck == 0) {
                System.out.println("Debug: Execution progress - Cycles: " + cycles + ", PC: " + pc + ", Current instruction: " + ins);
                System.out.println("Debug: Current variables: " + vars);
            }
            
            if (ins.isBasic()) {
                BasicInstruction b = (BasicInstruction) ins;
                String vName = b.getVariable().getName().toLowerCase(Locale.ROOT); // Normalize to lowercase
                switch (b.getOp()) {
                    case INCREASE: {
                        double val = vars.getOrDefault(vName, 0.0);
                        vars.put(vName, val + 1);
                        pc++;
                        break;
                    }
                    case DECREASE: {
                        double val = vars.getOrDefault(vName, 0.0);
                        // Allow negative values - remove the clamp to 0
                        vars.put(vName, val - 1);
                        pc++;
                        break;
                    }
                    case NEUTRAL: {
                        // no change
                        pc++;
                        break;
                    }
                    case JUMP_NOT_ZERO: {
                        double val = vars.getOrDefault(vName, 0.0);
                        if (val != 0) {
                            String lbl = b.getJumpLabel();
                            if (lbl != null) {
                                String key = lbl.toUpperCase(Locale.ROOT);
                                if ("EXIT".equals(key)) {
                                    pc = insList.size();
                                } else {
                                    Integer tgt = labelMap.get(key);
                                    if (tgt != null) {
                                        pc = tgt;
                                    } else {
                                        // undefined label – skip
                                        pc++;
                                    }
                                }
                            } else {
                                pc++;
                            }
                        } else {
                            pc++;
                        }
                        break;
                    }
                }
            } else {
                SyntheticInstruction s = (SyntheticInstruction) ins;
                String vName = s.getVariable().getName().toLowerCase(Locale.ROOT); // Normalize to lowercase
                switch (s.getOp()) {
                    case ZERO_VARIABLE: {
                        vars.put(vName, 0.0);
                        pc++;
                        break;
                    }
                    case ASSIGNMENT: {
                        String srcName = s.getArgs().get("assignedVariable");
                        if (srcName == null || srcName.trim().isEmpty()) {
                            System.out.println("ASSIGNMENT DEBUG: " + vName + " ← null/empty, setting to 0");
                            vars.put(vName, 0.0);
                        } else {
                            String key = srcName.trim().toLowerCase(Locale.ROOT);
                            double val = vars.getOrDefault(key, 0.0);
                            System.out.println("ASSIGNMENT DEBUG: " + vName + " ← " + key + " (value=" + val + ")");
                            System.out.println("ASSIGNMENT DEBUG: vars contains: " + vars.keySet());
                            vars.put(vName, val);
                        }
                        pc++;
                        break;
                    }
                    case CONSTANT_ASSIGNMENT: {
                        String cStr = s.getArgs().get("constantValue");
                        double cVal;
                        try {
                            cVal = (cStr == null) ? 0.0 : Double.parseDouble(cStr);
                        } catch (NumberFormatException ex) {
                            cVal = 0.0;
                        }
                        vars.put(vName, cVal);
                        pc++;
                        break;
                    }
                    case GOTO_LABEL: {
                        String lbl = s.getArgs().get("gotoLabel");
                        if (lbl != null) {
                            String key = lbl.toUpperCase(Locale.ROOT);
                            if ("EXIT".equals(key)) {
                                pc = insList.size();
                            } else {
                                Integer tgt = labelMap.get(key);
                                if (tgt != null) {
                                    pc = tgt;
                                } else {
                                    pc++;
                                }
                            }
                        } else {
                            pc++;
                        }
                        break;
                    }
                    case JUMP_ZERO: {
                        String lbl = s.getArgs().get("JZLabel");
                        double val = vars.getOrDefault(vName, 0.0);
                        if (val == 0) {
                            if (lbl != null) {
                                String key = lbl.toUpperCase(Locale.ROOT);
                                if ("EXIT".equals(key)) {
                                    pc = insList.size();
                                } else {
                                    Integer tgt = labelMap.get(key);
                                    if (tgt != null) {
                                        pc = tgt;
                                    } else {
                                        pc++;
                                    }
                                }
                            } else {
                                pc++;
                            }
                        } else {
                            pc++;
                        }
                        break;
                    }
                    case JUMP_EQUAL_CONSTANT: {
                        String lbl = s.getArgs().get("JEConstantLabel");
                        String cStr = s.getArgs().get("constantValue");
                        double cVal;
                        try {
                            cVal = (cStr == null) ? 0.0 : Double.parseDouble(cStr);
                        } catch (NumberFormatException ex) {
                            cVal = 0.0;
                        }
                        double val = vars.getOrDefault(vName, 0.0);
                        if (val == cVal) {
                            if (lbl != null) {
                                String key = lbl.toUpperCase(Locale.ROOT);
                                if ("EXIT".equals(key)) {
                                    pc = insList.size();
                                } else {
                                    Integer tgt = labelMap.get(key);
                                    if (tgt != null) {
                                        pc = tgt;
                                    } else {
                                        pc++;
                                    }
                                }
                            } else {
                                pc++;
                            }
                        } else {
                            pc++;
                        }
                        break;
                    }
                    case JUMP_EQUAL_VARIABLE: {
                        String lbl = s.getArgs().get("JEVariableLabel");
                        String cmpName = s.getArgs().get("variableName");
                        double v1 = vars.getOrDefault(vName, 0.0);
                        double v2 = 0.0;
                        if (cmpName != null) {
                            v2 = vars.getOrDefault(cmpName.trim().toLowerCase(Locale.ROOT), 0.0);
                        }
                        if (v1 == v2) {
                            if (lbl != null) {
                                String key = lbl.toUpperCase(Locale.ROOT);
                                if ("EXIT".equals(key)) {
                                    pc = insList.size();
                                } else {
                                    Integer tgt = labelMap.get(key);
                                    if (tgt != null) {
                                        pc = tgt;
                                    } else {
                                        pc++;
                                    }
                                }
                            } else {
                                pc++;
                            }
                        } else {
                            pc++;
                        }
                        break;
                    }
                    case QUOTE: {
                        // QUOTE instruction - implement actual function calls
                        String functionName = s.getArgs().get("functionName");
                        String functionArgs = s.getArgs().get("functionArguments");
                        
                        if (functionName != null) {
                            double result = executeFunction(functionName, functionArgs, vars);
                            vars.put(vName, result);
                        } else {
                            vars.put(vName, 0.0);
                        }
                        pc++;
                        break;
                    }
                    default: {
                        pc++;
                        break;
                    }
                }
            }
            if (pc == insList.size()) {
                // Program terminated by EXIT or by jumping past last line
                break;
            }
        }
        
        // Check if execution was terminated due to cycle limit
        if (cycles >= MAX_CYCLES) {
            throw new RuntimeException("Program execution exceeded maximum cycle limit (" + MAX_CYCLES + "). Possible infinite loop detected.");
        }
        
        double yVal = vars.getOrDefault("y", 0.0);
        // Record history
        runCounter++;
        HistoryEntry entry = new HistoryEntry(runCounter, d, inputs == null ? Collections.emptyList() : new ArrayList<>(inputs), yVal, cycles);
        history.add(entry);
        return new RunResult(yVal, new HashMap<>(vars), cycles);
    }
    
    /**
     * Execute a function called by QUOTE instruction
     */
    private double executeFunction(String functionName, String functionArgs, Map<String, Double> vars) {
        if (functionName == null) return 0.0;
        
        System.out.println("Debug: Executing function: " + functionName + " with args: " + functionArgs);
        
        // First check for user-defined functions
        if (currentProgram != null) {
            List<Function> functions = currentProgram.getFunctions();
            System.out.println("Debug: Available user-defined functions: " + functions.size());
            for (Function func : functions) {
                System.out.println("Debug: Function available: '" + func.getName() + "'");
                if (functionName.equals(func.getName())) {
                    // Found user-defined function - execute it
                    System.out.println("Debug: Found user-defined function: " + functionName);
                    return executeUserDefinedFunction(func, functionArgs, vars);
                }
            }
            System.out.println("Debug: No matching user-defined function found for: '" + functionName + "'");
        }
        
        // Parse function arguments for built-in functions
        List<Double> argValues = parseFunctionArguments(functionArgs, vars);
        System.out.println("Debug: Parsed argument values: " + argValues);
        
        double result = 0.0;
        
        // Check built-in functions
        switch (functionName) {
            case "Smaller_Than":
                // Returns 1 if arg1 < arg2, 0 otherwise
                if (argValues.size() >= 2) {
                    result = (argValues.get(0) < argValues.get(1)) ? 1.0 : 0.0;
                }
                break;
                
            case "Smaller_Equal_Than":
                // Returns 1 if arg1 <= arg2, 0 otherwise
                if (argValues.size() >= 2) {
                    result = (argValues.get(0) <= argValues.get(1)) ? 1.0 : 0.0;
                }
                break;
                
            case "EQUAL":
                // Returns 1 if arg1 == arg2, 0 otherwise
                if (argValues.size() >= 2) {
                    result = (argValues.get(0).equals(argValues.get(1))) ? 1.0 : 0.0;
                }
                break;
                
            case "NOT":
                // Returns 1 if arg == 0, 0 otherwise
                if (argValues.size() >= 1) {
                    result = (argValues.get(0) == 0.0) ? 1.0 : 0.0;
                } else {
                    result = 1.0;
                }
                break;
                
            case "AND":
                // Returns 1 if all args are non-zero, 0 otherwise
                result = 1.0; // Start with true
                for (Double val : argValues) {
                    if (val == 0.0) {
                        result = 0.0;
                        break;
                    }
                }
                if (argValues.isEmpty()) result = 0.0;
                break;
                
            case "Minus":
                // Returns arg1 - arg2 (ALLOW negative values - remove clamp!)
                if (argValues.size() >= 2) {
                    result = argValues.get(0) - argValues.get(1);
                }
                break;
                
            case "CONST0":
                result = 0.0;
                break;
                
            default:
                System.out.println("Warning: Unknown function: " + functionName);
                result = 0L;
        }
        
        System.out.println("Debug: Function " + functionName + " returned: " + result);
        return result;
    }
    
    /**
     * Execute a user-defined function
     */
    private double executeUserDefinedFunction(Function function, String functionArgs, Map<String, Double> vars) {
        System.out.println("Debug: Executing user-defined function: " + function.getName());
        
        // Parse function arguments to get input values
        List<Double> argValues = parseFunctionArguments(functionArgs, vars);
        System.out.println("Debug: Function args parsed to values: " + argValues);
        
        // Create a new variable context for the function execution
        Map<String, Double> functionVars = new HashMap<>();
        
        // Map arguments to x1, x2, x3... in the function context  
        for (int i = 0; i < argValues.size(); i++) {
            functionVars.put("x" + (i + 1), argValues.get(i));
        }
        
        // Initialize all other variables to 0
        functionVars.put("y", 0.0);  // Function result
        for (int i = 1; i <= 10; i++) {  // Initialize z1-z10
            functionVars.put("z" + i, 0.0);
        }
        
        System.out.println("Debug: Function initial variables: " + functionVars);
        
        // Execute the function's instructions
        List<Instruction> instructions = function.getInstructions();
        
        // Build label map for the function
        Map<String, Integer> labelMap = new HashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instr = instructions.get(i);
            if (instr.getLabel() != null && !instr.getLabel().trim().isEmpty()) {
                labelMap.put(instr.getLabel().trim(), i);
            }
        }
        
        int pc = 0;
        int maxCycles = 10000; // Prevent infinite loops
        int cycles = 0;
        
        while (pc < instructions.size() && cycles < maxCycles) {
            cycles++;
            Instruction instr = instructions.get(pc);
            
            System.out.println("Debug: Function PC=" + pc + " executing: " + instr.getClass().getSimpleName());
            
            if (instr instanceof BasicInstruction) {
                BasicInstruction b = (BasicInstruction) instr;
                String varName = b.getVariable().toString();
                BasicInstruction.Op op = b.getOp();
                
                switch (op) {
                    case INCREASE:
                        functionVars.put(varName, functionVars.getOrDefault(varName, 0.0) + 1);
                        break;
                    case DECREASE:
                        // Allow negative values - remove Math.max restriction
                        functionVars.put(varName, functionVars.getOrDefault(varName, 0.0) - 1);
                        break;
                    case JUMP_NOT_ZERO:
                        if (functionVars.getOrDefault(varName, 0.0) != 0.0) {
                            // Skip next instruction
                            pc++;
                        }
                        break;
                    case NEUTRAL:
                        // Do nothing
                        break;
                }
                pc++;
                
            } else if (instr instanceof SyntheticInstruction) {
                SyntheticInstruction s = (SyntheticInstruction) instr;
                String varName = s.getVariable().toString();
                SyntheticInstruction.Op op = s.getOp();
                Map<String, String> args = s.getArgs();
                
                switch (op) {
                    case ASSIGNMENT:
                        String sourceVar = args.get("assignedVariable");
                        if (sourceVar != null) {
                            double value = functionVars.getOrDefault(sourceVar, 0.0);
                            functionVars.put(varName, value);
                            System.out.println("Debug: Function assignment " + varName + " = " + value);
                        }
                        break;
                        
                    case CONSTANT_ASSIGNMENT:
                        String constValue = args.get("constantValue");
                        if (constValue != null) {
                            try {
                                double value = Double.parseDouble(constValue);
                                functionVars.put(varName, value);
                                System.out.println("Debug: Function constant assignment " + varName + " = " + value);
                            } catch (NumberFormatException e) {
                                System.out.println("Debug: Invalid constant value: " + constValue);
                            }
                        }
                        break;
                        
                    case GOTO_LABEL:
                        String gotoLabel = args.get("gotoLabel");
                        if ("EXIT".equals(gotoLabel)) {
                            pc = instructions.size(); // Exit function
                            continue;
                        } else if (gotoLabel != null && labelMap.containsKey(gotoLabel)) {
                            pc = labelMap.get(gotoLabel);
                            continue;
                        }
                        break;
                        
                    case QUOTE:
                        // Nested function call within user function
                        String nestedFunctionName = args.get("functionName");
                        String nestedFunctionArgs = args.get("functionArguments");
                        if (nestedFunctionName != null) {
                            double result = executeFunction(nestedFunctionName, nestedFunctionArgs, functionVars);
                            functionVars.put(varName, result);
                            System.out.println("Debug: Function nested call result " + varName + " = " + result);
                        }
                        break;
                        
                    case ZERO_VARIABLE:
                        functionVars.put(varName, 0.0);
                        break;
                        
                    case JUMP_ZERO:
                        String jzLabel = args.get("JZLabel");
                        if (functionVars.getOrDefault(varName, 0.0) == 0.0) {
                            if ("EXIT".equals(jzLabel)) {
                                pc = instructions.size(); // Exit function
                                continue;
                            } else if (jzLabel != null && labelMap.containsKey(jzLabel)) {
                                pc = labelMap.get(jzLabel);
                                continue;
                            }
                        }
                        break;
                        
                    case JUMP_EQUAL_CONSTANT:
                        String constVal = args.get("constantValue");
                        if (constVal != null) {
                            try {
                                double constant = Double.parseDouble(constVal);
                                if (functionVars.getOrDefault(varName, 0.0) == constant) {
                                    String jumpLabel = args.get("JECLabel");
                                    if ("EXIT".equals(jumpLabel)) {
                                        pc = instructions.size(); // Exit function
                                        continue;
                                    } else if (jumpLabel != null && labelMap.containsKey(jumpLabel)) {
                                        pc = labelMap.get(jumpLabel);
                                        continue;
                                    }
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Debug: Invalid constant value: " + constVal);
                            }
                        }
                        break;
                        
                    case JUMP_EQUAL_VARIABLE:
                        String compareVar = args.get("compareVariable");
                        if (compareVar != null) {
                            if (functionVars.getOrDefault(varName, 0.0).equals(functionVars.getOrDefault(compareVar, 0.0))) {
                                String jumpLabel = args.get("JEVLabel");
                                if ("EXIT".equals(jumpLabel)) {
                                    pc = instructions.size(); // Exit function
                                    continue;
                                } else if (jumpLabel != null && labelMap.containsKey(jumpLabel)) {
                                    pc = labelMap.get(jumpLabel);
                                    continue;
                                }
                            }
                        }
                        break;
                }
                pc++;
            }
        }
        
        double result = functionVars.getOrDefault("y", 0.0);
        System.out.println("Debug: User function " + function.getName() + " returned: " + result);
        return result;
    }
    
    /**
     * Parse function arguments from string format like "x1,x2" or "(Smaller_Equal_Than,z3,x2),(NOT,(EQUAL,z3,(CONST0)))"
     */
    private List<Double> parseFunctionArguments(String functionArgs, Map<String, Double> vars) {
        List<Double> values = new ArrayList<>();
        
        if (functionArgs == null || functionArgs.trim().isEmpty()) {
            return values;
        }
        
        String args = functionArgs.trim();
        System.out.println("Debug: Parsing function args: " + args);
        
        // Simple case: comma-separated variable names like "x1,x2"
        if (!args.contains("(")) {
            String[] argList = args.split(",");
            for (String arg : argList) {
                String varName = arg.trim().toLowerCase();
                Double value = vars.get(varName);
                double val = (value != null) ? value : 0.0;
                values.add(val);
                System.out.println("Debug: Variable " + varName + " = " + val);
            }
        } else {
            // Complex case: nested function calls like "(Smaller_Equal_Than,z3,x2),(NOT,(EQUAL,z3,(CONST0)))"
            List<String> expressions = parseNestedExpressions(args);
            System.out.println("Debug: Parsed expressions: " + expressions);
            for (String expr : expressions) {
                double result = evaluateExpression(expr, vars);
                values.add(result);
                System.out.println("Debug: Expression '" + expr + "' = " + result);
            }
        }
        
        return values;
    }
    
    /**
     * Parse nested expressions separated by commas at the top level
     */
    private List<String> parseNestedExpressions(String args) {
        List<String> expressions = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        
        for (char c : args.toCharArray()) {
            if (c == '(') {
                depth++;
                current.append(c);
            } else if (c == ')') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                // Top-level comma - end current expression
                if (current.length() > 0) {
                    expressions.add(current.toString().trim());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        
        // Add final expression
        if (current.length() > 0) {
            expressions.add(current.toString().trim());
        }
        
        return expressions;
    }
    
    /**
     * Evaluate a single expression like "(Smaller_Equal_Than,z3,x2)" or "x1"
     */
    private double evaluateExpression(String expr, Map<String, Double> vars) {
        expr = expr.trim();
        
        // Simple variable reference
        if (!expr.contains("(") && !expr.contains(",")) {
            String varName = expr.toLowerCase();
            return vars.getOrDefault(varName, 0.0);
        }
        
        // Function call like "(Smaller_Equal_Than,z3,x2)"
        if (expr.startsWith("(") && expr.endsWith(")")) {
            String inner = expr.substring(1, expr.length() - 1);
            String[] parts = inner.split(",", 2); // Split only on first comma
            
            if (parts.length >= 1) {
                String functionName = parts[0].trim();
                String functionArgs = parts.length > 1 ? parts[1] : "";
                
                return executeFunction(functionName, functionArgs, vars);
            }
        }
        
        return 0.0;
    }

    @Override
    public List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    @Override
    public List<String> getInputVariables() {
        if (currentProgram == null) {
            return Collections.emptyList();
        }
        return currentProgram.getInputVariables();
    }

    @Override
    public String getProgramName() {
        if (currentProgram == null) {
            return null;
        }
        return currentProgram.getName();
    }

    @Override
    public List<Function> getFunctions() {
        if (currentProgram == null) {
            return Collections.emptyList();
        }
        return currentProgram.getFunctions();
    }

    /**
     * Validates that all function references in QUOTE instructions are either built-in
     * functions or defined user functions. Throws an exception if undefined functions are found.
     */
    private void validateFunctionReferences(List<Instruction> instructions, List<Function> functions) throws Exception {
        // Create set of defined function names for quick lookup
        Set<String> definedFunctions = new HashSet<>();
        for (Function func : functions) {
            definedFunctions.add(func.getName());
        }
        
        // Add built-in function names
        Set<String> builtInFunctions = new HashSet<>();
        builtInFunctions.add("Smaller_Than");
        builtInFunctions.add("Smaller_Equal_Than");
        builtInFunctions.add("EQUAL");
        builtInFunctions.add("NOT");
        builtInFunctions.add("AND");
        builtInFunctions.add("Minus");
        builtInFunctions.add("CONST0");
        
        // Check all QUOTE instructions for undefined function references
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instr = instructions.get(i);
            if (instr instanceof SyntheticInstruction) {
                SyntheticInstruction synInstr = (SyntheticInstruction) instr;
                if (synInstr.getOp() == SyntheticInstruction.Op.QUOTE) {
                    String functionName = synInstr.getArgs().get("functionName");
                    if (functionName != null && !functionName.trim().isEmpty()) {
                        functionName = functionName.trim();
                        // Check if function is defined (either built-in or user-defined)
                        if (!builtInFunctions.contains(functionName) && !definedFunctions.contains(functionName)) {
                            throw new Exception("Undefined function '" + functionName + "' referenced at instruction " + (i + 1));
                        }
                    }
                }
            }
        }
    }

    /**
     * Parses an XML program file.  Performs structural and semantic
     * validation according to the assignment specification.  On error
     * throws an exception with a user friendly message.
     */
    private Program parseProgram(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        Element root = doc.getDocumentElement();
        if (root == null || !"S-Program".equals(root.getNodeName())) {
            throw new Exception("Root element must be S-Program");
        }
        String progName = root.getAttribute("name");
        if (progName == null || progName.trim().isEmpty()) {
            throw new Exception("Program name is missing");
        }
        
        // Fetch instruction nodes ONLY from the main S-Instructions section (direct child of S-Program, not from S-Function sections)
        NodeList instructionsSections = root.getChildNodes();
        Element mainInstructions = null;
        for (int i = 0; i < instructionsSections.getLength(); i++) {
            Node node = instructionsSections.item(i);
            if (node instanceof Element && "S-Instructions".equals(node.getNodeName())) {
                mainInstructions = (Element) node;
                break;
            }
        }
        if (mainInstructions == null) {
            throw new Exception("Program must have a main S-Instructions section");
        }
        NodeList instrNodes = mainInstructions.getElementsByTagName("S-Instruction");
        
        List<Instruction> instructions = new ArrayList<>();
        Set<String> definedLabels = new HashSet<>();
        int maxLabel = 0;
        int maxZ = 0;
        
        // DEBUG: Check what instruction nodes were found
        System.out.println("=== INSTRUCTION PARSING DEBUG ===");
        System.out.println("Total S-Instruction nodes found: " + instrNodes.getLength());
        
        // First pass: build instructions and collect labels
        for (int i = 0; i < instrNodes.getLength(); i++) {
            Node node = instrNodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element instrElem = (Element) node;
            String typeStr = instrElem.getAttribute("type");
            String nameStr = instrElem.getAttribute("name");
            int lineIndex = i + 1;
            
            // DEBUG: Print each instruction being parsed
            System.out.println("Parsing instruction " + lineIndex + ": type=" + typeStr + ", name=" + nameStr);
            // Parse variable
            NodeList varNodes = instrElem.getElementsByTagName("S-Variable");
            if (varNodes.getLength() != 1) {
                throw new Exception("Instruction " + lineIndex + " must have exactly one S-Variable");
            }
            String varText = varNodes.item(0).getTextContent();
            VariableRef varRef;
            try {
                varRef = VariableRef.fromString(varText);
            } catch (IllegalArgumentException ex) {
                throw new Exception("Invalid variable name '" + varText + "' at instruction " + lineIndex);
            }
            if (varRef.getType() == VariableRef.Type.Z && varRef.getIndex() > maxZ) {
                maxZ = varRef.getIndex();
            }
            // Parse label
            NodeList labelNodes = instrElem.getElementsByTagName("S-Label");
            String label = null;
            if (labelNodes.getLength() == 1) {
                String lblText = labelNodes.item(0).getTextContent();
                if (lblText != null) {
                    lblText = lblText.trim();
                    if (!lblText.isEmpty()) {
                        label = lblText;
                        definedLabels.add(label);
                        String upLabel = label.toUpperCase(Locale.ROOT);
                        if (upLabel.startsWith("L")) {
                            try {
                                int num = Integer.parseInt(upLabel.substring(1));
                                if (num > maxLabel) {
                                    maxLabel = num;
                                }
                            } catch (NumberFormatException ignore) {
                                // ignore
                            }
                        }
                    }
                }
            }
            // Parse arguments
            Map<String, String> args = new HashMap<>();
            NodeList argGroup = instrElem.getElementsByTagName("S-Instruction-Arguments");
            if (argGroup.getLength() == 1) {
                Node argNode = argGroup.item(0);
                if (argNode instanceof Element) {
                    NodeList argList = ((Element) argNode).getElementsByTagName("S-Instruction-Argument");
                    for (int a = 0; a < argList.getLength(); a++) {
                        Node argEntry = argList.item(a);
                        if (argEntry instanceof Element) {
                            Element argElem = (Element) argEntry;
                            String argName = argElem.getAttribute("name");
                            String argVal = argElem.getAttribute("value");
                            args.put(argName, argVal);
                        }
                    }
                }
            }
            // Determine instruction type and create instruction
            if (typeStr == null) {
                throw new Exception("Instruction " + lineIndex + " missing type attribute");
            }
            String type = typeStr.trim().toLowerCase(Locale.ROOT);
            String opName = (nameStr == null) ? "" : nameStr.trim().toUpperCase(Locale.ROOT);
            if ("basic".equals(type)) {
                // Map opName to basic op
                BasicInstruction.Op op;
                String jumpLabel = null;
                switch (opName) {
                    case "INCREASE":
                        op = BasicInstruction.Op.INCREASE;
                        break;
                    case "DECREASE":
                        op = BasicInstruction.Op.DECREASE;
                        break;
                    case "NEUTRAL":
                        op = BasicInstruction.Op.NEUTRAL;
                        break;
                    case "JUMP_NOT_ZERO":
                        op = BasicInstruction.Op.JUMP_NOT_ZERO;
                        jumpLabel = args.get("JNZLabel");
                        if (jumpLabel == null || jumpLabel.trim().isEmpty()) {
                            throw new Exception("Instruction " + lineIndex + " of type JUMP_NOT_ZERO must specify JNZLabel");
                        }
                        jumpLabel = jumpLabel.trim();
                        break;
                    default:
                        throw new Exception("Unknown basic instruction name '" + opName + "' at instruction " + lineIndex);
                }
                BasicInstruction basic = new BasicInstruction(label, lineIndex, op, varRef, jumpLabel);
                instructions.add(basic);
            } else if ("synthetic".equals(type)) {
                SyntheticInstruction.Op op;
                switch (opName) {
                    case "ZERO_VARIABLE":
                        op = SyntheticInstruction.Op.ZERO_VARIABLE;
                        break;
                    case "ASSIGNMENT":
                        op = SyntheticInstruction.Op.ASSIGNMENT;
                        break;
                    case "CONSTANT_ASSIGNMENT":
                        op = SyntheticInstruction.Op.CONSTANT_ASSIGNMENT;
                        break;
                    case "GOTO_LABEL":
                        op = SyntheticInstruction.Op.GOTO_LABEL;
                        break;
                    case "JUMP_ZERO":
                        op = SyntheticInstruction.Op.JUMP_ZERO;
                        break;
                    case "JUMP_EQUAL_CONSTANT":
                        op = SyntheticInstruction.Op.JUMP_EQUAL_CONSTANT;
                        break;
                    case "JUMP_EQUAL_VARIABLE":
                        op = SyntheticInstruction.Op.JUMP_EQUAL_VARIABLE;
                        break;
                    case "QUOTE":
                        op = SyntheticInstruction.Op.QUOTE;
                        break;
                    default:
                        throw new Exception("Unknown synthetic instruction name '" + opName + "' at instruction " + lineIndex);
                }
                // validate presence of required arguments
                Map<String, String> argsCopy = new HashMap<>(args);
                SyntheticInstruction syn = new SyntheticInstruction(label, lineIndex, op, varRef, argsCopy);
                instructions.add(syn);
            } else {
                throw new Exception("Unknown instruction type '" + typeStr + "' at instruction " + lineIndex);
            }
        }
        // Build label map (case insensitive keys)
        Map<String, Integer> labelMap = new HashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
            String lbl = ins.getLabel();
            if (lbl != null) {
                String key = lbl.toUpperCase(Locale.ROOT);
                // use first occurrence
                labelMap.putIfAbsent(key, i);
            }
        }
        // Validate label references and argument consistency
        for (int i = 0; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
            if (ins.isBasic()) {
                BasicInstruction b = (BasicInstruction) ins;
                if (b.getOp() == BasicInstruction.Op.JUMP_NOT_ZERO) {
                    String lbl = b.getJumpLabel();
                    String key = lbl.toUpperCase(Locale.ROOT);
                    if (!"EXIT".equals(key) && !labelMap.containsKey(key)) {
                        throw new Exception("Invalid jump label '" + lbl + "' at instruction " + (i + 1));
                    }
                }
            } else {
                SyntheticInstruction s = (SyntheticInstruction) ins;
                switch (s.getOp()) {
                    case GOTO_LABEL: {
                        String lbl = s.getArgs().get("gotoLabel");
                        if (lbl == null || lbl.trim().isEmpty()) {
                            throw new Exception("Instruction " + (i + 1) + " of type GOTO_LABEL must specify gotoLabel");
                        }
                        String key = lbl.trim().toUpperCase(Locale.ROOT);
                        if (!"EXIT".equals(key) && !labelMap.containsKey(key)) {
                            throw new Exception("Invalid goto label '" + lbl + "' at instruction " + (i + 1));
                        }
                        break;
                    }
                    case JUMP_ZERO: {
                        String lbl = s.getArgs().get("JZLabel");
                        if (lbl == null || lbl.trim().isEmpty()) {
                            throw new Exception("Instruction " + (i + 1) + " of type JUMP_ZERO must specify JZLabel");
                        }
                        String key = lbl.trim().toUpperCase(Locale.ROOT);
                        if (!"EXIT".equals(key) && !labelMap.containsKey(key)) {
                            throw new Exception("Invalid jump label '" + lbl + "' at instruction " + (i + 1));
                        }
                        break;
                    }
                    case JUMP_EQUAL_CONSTANT: {
                        String lbl = s.getArgs().get("JEConstantLabel");
                        if (lbl == null || lbl.trim().isEmpty()) {
                            throw new Exception("Instruction " + (i + 1) + " of type JUMP_EQUAL_CONSTANT must specify JEConstantLabel");
                        }
                        String key = lbl.trim().toUpperCase(Locale.ROOT);
                        if (!"EXIT".equals(key) && !labelMap.containsKey(key)) {
                            throw new Exception("Invalid jump label '" + lbl + "' at instruction " + (i + 1));
                        }
                        String cStr = s.getArgs().get("constantValue");
                        if (cStr == null) {
                            throw new Exception("Instruction " + (i + 1) + " of type JUMP_EQUAL_CONSTANT must specify constantValue");
                        }
                        try {
                            Long.parseLong(cStr);
                        } catch (NumberFormatException ex) {
                            throw new Exception("Invalid constantValue '" + cStr + "' at instruction " + (i + 1));
                        }
                        break;
                    }
                    case JUMP_EQUAL_VARIABLE: {
                        String lbl = s.getArgs().get("JEVariableLabel");
                        if (lbl == null || lbl.trim().isEmpty()) {
                            throw new Exception("Instruction " + (i + 1) + " of type JUMP_EQUAL_VARIABLE must specify JEVariableLabel");
                        }
                        String key = lbl.trim().toUpperCase(Locale.ROOT);
                        if (!"EXIT".equals(key) && !labelMap.containsKey(key)) {
                            throw new Exception("Invalid jump label '" + lbl + "' at instruction " + (i + 1));
                        }
                        String cmpVar = s.getArgs().get("variableName");
                        if (cmpVar == null || cmpVar.trim().isEmpty()) {
                            throw new Exception("Instruction " + (i + 1) + " of type JUMP_EQUAL_VARIABLE must specify variableName");
                        }
                        try {
                            VariableRef.fromString(cmpVar);
                        } catch (IllegalArgumentException ex) {
                            throw new Exception("Invalid variableName '" + cmpVar + "' at instruction " + (i + 1));
                        }
                        break;
                    }
                    case ASSIGNMENT: {
                        // Nothing special; assignedVariable can be absent (treated as zero)
                        break;
                    }
                    case CONSTANT_ASSIGNMENT: {
                        String cStr = s.getArgs().get("constantValue");
                        if (cStr == null) {
                            throw new Exception("Instruction " + (i + 1) + " of type CONSTANT_ASSIGNMENT must specify constantValue");
                        }
                        try {
                            Long.parseLong(cStr);
                        } catch (NumberFormatException ex) {
                            throw new Exception("Invalid constantValue '" + cStr + "' at instruction " + (i + 1));
                        }
                        break;
                    }
                    case ZERO_VARIABLE: {
                        // ZERO_VARIABLE requires no additional arguments
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        // Determine the set of input variables (x1,x2,...) referenced in the program.  We
        // collect all variable references from instructions and certain arguments, then
        // sort them by index to form a stable list.  Only variables of type X are
        // included.
        Set<Integer> xIndices = new HashSet<>();
        for (Instruction instr : instructions) {
            // primary variable of instruction
            VariableRef ref = null;
            if (instr instanceof BasicInstruction) {
                ref = ((BasicInstruction) instr).getVariable();
            } else if (instr instanceof SyntheticInstruction) {
                ref = ((SyntheticInstruction) instr).getVariable();
            }
            if (ref != null && ref.getType() == VariableRef.Type.X) {
                xIndices.add(ref.getIndex());
            }
            // inspect arguments that refer to variables
            if (instr instanceof SyntheticInstruction) {
                SyntheticInstruction s = (SyntheticInstruction) instr;
                Map<String, String> a = s.getArgs();
                // assignedVariable in ASSIGNMENT
                String assigned = a.get("assignedVariable");
                if (assigned != null && !assigned.trim().isEmpty()) {
                    try {
                        VariableRef tmp = VariableRef.fromString(assigned.trim());
                        if (tmp.getType() == VariableRef.Type.X) {
                            xIndices.add(tmp.getIndex());
                        }
                    } catch (IllegalArgumentException ignore) {
                        // ignore invalid names here; they are handled elsewhere
                    }
                }
                // variableName in JUMP_EQUAL_VARIABLE
                String cmpName = a.get("variableName");
                if (cmpName != null && !cmpName.trim().isEmpty()) {
                    try {
                        VariableRef tmp = VariableRef.fromString(cmpName.trim());
                        if (tmp.getType() == VariableRef.Type.X) {
                            xIndices.add(tmp.getIndex());
                        }
                    } catch (IllegalArgumentException ignore) {
                        // ignore invalid names here; they are handled elsewhere
                    }
                }
                // functionArguments in QUOTE - scan for variable references like x1, x2, etc.
                String funcArgs = a.get("functionArguments");
                if (funcArgs != null && !funcArgs.trim().isEmpty()) {
                    // Scan for variable patterns like x1, x2, etc. in function argument strings
                    String argText = funcArgs.trim();
                    // Look for x followed by digits (x1, x2, x3, etc.)
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\bx(\\d+)\\b");
                    java.util.regex.Matcher matcher = pattern.matcher(argText);
                    while (matcher.find()) {
                        try {
                            int index = Integer.parseInt(matcher.group(1));
                            xIndices.add(index);
                        } catch (NumberFormatException ignore) {
                            // ignore invalid numbers
                        }
                    }
                }
            }
        }
        List<Integer> sortedX = new ArrayList<>(xIndices);
        Collections.sort(sortedX);
        List<String> inputVars = new ArrayList<>();
        for (Integer idx : sortedX) {
            inputVars.add("x" + idx);
        }

        // Parse functions from S-Functions section
        List<Function> functions = new ArrayList<>();
        NodeList functionNodes = root.getElementsByTagName("S-Function");
        for (int i = 0; i < functionNodes.getLength(); i++) {
            Node node = functionNodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element funcElem = (Element) node;
            String funcName = funcElem.getAttribute("name");
            String userString = funcElem.getAttribute("user-string");
            
            if (funcName == null || funcName.trim().isEmpty()) {
                throw new Exception("Function " + (i + 1) + " is missing name attribute");
            }
            if (userString == null || userString.trim().isEmpty()) {
                throw new Exception("Function " + (i + 1) + " is missing user-string attribute");
            }

            // Parse function instructions
            NodeList funcInstrNodes = funcElem.getElementsByTagName("S-Instruction");
            List<Instruction> functionInstructions = new ArrayList<>();
            
            // Parse function instructions similar to main program instructions
            for (int j = 0; j < funcInstrNodes.getLength(); j++) {
                Node instrNode = funcInstrNodes.item(j);
                if (!(instrNode instanceof Element)) {
                    continue;
                }
                Element instrElem = (Element) instrNode;
                
                // Parse this instruction (similar logic to main program parsing)
                String typeStr = instrElem.getAttribute("type");
                String nameStr = instrElem.getAttribute("name");
                int lineIndex = j + 1;
                
                // Parse variable
                NodeList varNodes = instrElem.getElementsByTagName("S-Variable");
                if (varNodes.getLength() != 1) {
                    throw new Exception("Function " + funcName + " instruction " + lineIndex + " must have exactly one S-Variable");
                }
                String varText = varNodes.item(0).getTextContent();
                VariableRef varRef;
                try {
                    varRef = VariableRef.fromString(varText);
                } catch (IllegalArgumentException ex) {
                    throw new Exception("Invalid variable name '" + varText + "' in function " + funcName + " at instruction " + lineIndex);
                }
                
                // Parse arguments
                Map<String, String> args = new HashMap<>();
                NodeList argNodes = instrElem.getElementsByTagName("S-Instruction-Argument");
                for (int k = 0; k < argNodes.getLength(); k++) {
                    Element argElem = (Element) argNodes.item(k);
                    String argName = argElem.getAttribute("name");
                    String argValue = argElem.getAttribute("value");
                    if (argName != null && !argName.trim().isEmpty()) {
                        args.put(argName, argValue != null ? argValue : "");
                    }
                }
                
                // Create instruction based on type
                if ("basic".equals(typeStr)) {
                    BasicInstruction.Op op;
                    switch (nameStr) {
                        case "INCREASE":
                            op = BasicInstruction.Op.INCREASE;
                            break;
                        case "DECREASE":
                            op = BasicInstruction.Op.DECREASE;
                            break;
                        case "JUMP_NOT_ZERO":
                            op = BasicInstruction.Op.JUMP_NOT_ZERO;
                            break;
                        case "NEUTRAL":
                            op = BasicInstruction.Op.NEUTRAL;
                            break;
                        default:
                            throw new Exception("Unknown basic instruction name '" + nameStr + "' in function " + funcName + " at instruction " + lineIndex);
                    }
                    
                    // Parse label
                    NodeList labelNodes = instrElem.getElementsByTagName("S-Label");
                    String label = null;
                    if (labelNodes.getLength() > 0) {
                        label = labelNodes.item(0).getTextContent();
                    }
                    
                    String jumpLabel = null;
                    if (op == BasicInstruction.Op.JUMP_NOT_ZERO) {
                        jumpLabel = args.get("JNZLabel");
                    }
                    
                    BasicInstruction basic = new BasicInstruction(label, lineIndex, op, varRef, jumpLabel);
                    functionInstructions.add(basic);
                    
                } else if ("synthetic".equals(typeStr)) {
                    SyntheticInstruction.Op op;
                    switch (nameStr) {
                        case "ZERO_VARIABLE":
                            op = SyntheticInstruction.Op.ZERO_VARIABLE;
                            break;
                        case "ASSIGNMENT":
                            op = SyntheticInstruction.Op.ASSIGNMENT;
                            break;
                        case "CONSTANT_ASSIGNMENT":
                            op = SyntheticInstruction.Op.CONSTANT_ASSIGNMENT;
                            break;
                        case "GOTO_LABEL":
                            op = SyntheticInstruction.Op.GOTO_LABEL;
                            break;
                        case "JUMP_ZERO":
                            op = SyntheticInstruction.Op.JUMP_ZERO;
                            break;
                        case "JUMP_EQUAL_CONSTANT":
                            op = SyntheticInstruction.Op.JUMP_EQUAL_CONSTANT;
                            break;
                        case "JUMP_EQUAL_VARIABLE":
                            op = SyntheticInstruction.Op.JUMP_EQUAL_VARIABLE;
                            break;
                        case "QUOTE":
                            op = SyntheticInstruction.Op.QUOTE;
                            break;
                        default:
                            throw new Exception("Unknown synthetic instruction name '" + nameStr + "' in function " + funcName + " at instruction " + lineIndex);
                    }
                    
                    // Parse label
                    NodeList labelNodes = instrElem.getElementsByTagName("S-Label");
                    String label = null;
                    if (labelNodes.getLength() > 0) {
                        label = labelNodes.item(0).getTextContent();
                    }
                    
                    Map<String, String> argsCopy = new HashMap<>(args);
                    SyntheticInstruction syn = new SyntheticInstruction(label, lineIndex, op, varRef, argsCopy);
                    functionInstructions.add(syn);
                }
            }
            
            Function function = new Function(funcName.trim(), userString.trim(), functionInstructions);
            functions.add(function);
        }

        // Validate function references in QUOTE instructions
        validateFunctionReferences(instructions, functions);

        return new Program(progName, instructions, labelMap, maxLabel, maxZ, inputVars, functions);
    }

    /**
     * Helper to format labels into a 5‑character field for display.  Null
     * labels are represented as five spaces.  Labels longer than 5
     * characters are truncated.
     */
    private String formatLabelField(String label) {
        if (label == null || label.isEmpty()) {
            return "     ";
        }
        String trimmed = label.trim();
        if (trimmed.length() > 5) {
            trimmed = trimmed.substring(0, 5);
        }
        return String.format("%-5s", trimmed);
    }

    /**
     * Formats a basic instruction into a human friendly command string.
     */
    private String formatBasic(BasicInstruction b) {
        String var = b.getVariable().getName();
        switch (b.getOp()) {
            case INCREASE:
                return var + " <- " + var + " + 1";
            case DECREASE:
                return var + " <- " + var + " - 1";
            case NEUTRAL:
                return var + " <- " + var;
            case JUMP_NOT_ZERO:
                String lbl = b.getJumpLabel();
                return "IF " + var + " != 0 GOTO " + lbl;
            default:
                return "";
        }
    }

    /**
     * Formats a synthetic instruction into a human friendly command string.
     */
    private String formatSynthetic(SyntheticInstruction s) {
        String var = s.getVariable().getName();
        Map<String, String> args = s.getArgs();
        switch (s.getOp()) {
            case ZERO_VARIABLE:
                return var + " <- 0";
            case ASSIGNMENT: {
                String src = args.get("assignedVariable");
                if (src == null || src.trim().isEmpty()) {
                    return var + " <- 0";
                }
                return var + " <- " + src;
            }
            case CONSTANT_ASSIGNMENT: {
                String cStr = args.get("constantValue");
                if (cStr == null) {
                    cStr = "0";
                }
                return var + " <- " + cStr;
            }
            case GOTO_LABEL: {
                String lbl = args.get("gotoLabel");
                return "GOTO " + lbl;
            }
            case JUMP_ZERO: {
                String lbl = args.get("JZLabel");
                return "IF " + var + " = 0 GOTO " + lbl;
            }
            case JUMP_EQUAL_CONSTANT: {
                String lbl = args.get("JEConstantLabel");
                String cStr = args.get("constantValue");
                return "IF " + var + " = " + cStr + " GOTO " + lbl;
            }
            case JUMP_EQUAL_VARIABLE: {
                String lbl = args.get("JEVariableLabel");
                String cmp = args.get("variableName");
                return "IF " + var + " = " + cmp + " GOTO " + lbl;
            }
            case QUOTE: {
                String functionName = args.get("functionName");
                String functionArgs = args.get("functionArguments");
                if (functionName == null) functionName = "?";
                if (functionArgs == null) functionArgs = "()";
                return var + " <- " + functionName + functionArgs;
            }
            default:
                return "";
        }
    }

    // Data class for a single expanded line
    private static final class ExpandedLine {
        String label;
        String command;
        long cost;
        List<Integer> origins;
        int newLabelsUsed;
        int newWorkVarsUsed;
    }

    /**
     * Creates an expanded line helper.
     */
    private ExpandedLine makeLine(String label, String command, long cost, int origIndex, int newLabelsUsed, int newWorkVarsUsed) {
        ExpandedLine line = new ExpandedLine();
        line.label = label;
        line.command = command;
        line.cost = cost;
        line.origins = Collections.singletonList(origIndex);
        line.newLabelsUsed = newLabelsUsed;
        line.newWorkVarsUsed = newWorkVarsUsed;
        return line;
    }

    /**
     * Expands a synthetic instruction into a sequence of primitive lines.
     * Each expanded line carries information about how many new numeric
     * labels and work variables were consumed in order to update the
     * global counters correctly.  The origIndex identifies the original
     * program line for ancestry tracking.  The origLabel is attached to
     * the first expanded line where appropriate.
     */
    private List<ExpandedLine> expandSyntheticInstruction(SyntheticInstruction s, int origIndex, String origLabel,
                                                         int nextLabelIndex, int nextWorkVarIndex) {
        List<ExpandedLine> result = new ArrayList<>();
        switch (s.getOp()) {
            case ZERO_VARIABLE: {
                String dest = s.getVariable().getName();
                // Allocate a fresh zero variable
                String fresh = "z" + nextWorkVarIndex;
                // Copy dest <- fresh
                List<ExpandedLine> lines = expandAssignment(dest, fresh, origIndex, origLabel, nextLabelIndex, nextWorkVarIndex + 1);
                result.addAll(lines);
                break;
            }
            case CONSTANT_ASSIGNMENT: {
                String dest = s.getVariable().getName();
                String cStr = s.getArgs().get("constantValue");
                long cVal;
                try {
                    cVal = (cStr == null) ? 0L : Long.parseLong(cStr);
                } catch (NumberFormatException ex) {
                    cVal = 0L;
                }
                if (cVal <= 0) {
                    // treat as ZERO
                    result.addAll(expandSyntheticInstruction(new SyntheticInstruction(origLabel, origIndex, SyntheticInstruction.Op.ZERO_VARIABLE, s.getVariable(), Collections.emptyMap()), origIndex, origLabel, nextLabelIndex, nextWorkVarIndex));
                } else {
                    // dest = 0
                    List<ExpandedLine> zeroLines = expandSyntheticInstruction(new SyntheticInstruction(origLabel, origIndex, SyntheticInstruction.Op.ZERO_VARIABLE, s.getVariable(), Collections.emptyMap()), origIndex, origLabel, nextLabelIndex, nextWorkVarIndex);
                    // Add zero lines
                    for (ExpandedLine l : zeroLines) {
                        result.add(l);
                    }
                    // increments dest c times
                    for (int i = 0; i < cVal; i++) {
                        ExpandedLine inc = makeLine(null, dest + " <- " + dest + " + 1", 1, origIndex, 0, 0);
                        result.add(inc);
                    }
                    // final neutral
                    ExpandedLine neutral = makeLine(null, dest + " <- " + dest, 1, origIndex, 0, 0);
                    result.add(neutral);
                }
                break;
            }
            case ASSIGNMENT: {
                String dest = s.getVariable().getName();
                String src = s.getArgs().get("assignedVariable");
                if (src == null || src.trim().isEmpty()) {
                    // Equivalent to ZERO
                    result.addAll(expandSyntheticInstruction(new SyntheticInstruction(origLabel, origIndex, SyntheticInstruction.Op.ZERO_VARIABLE, s.getVariable(), Collections.emptyMap()), origIndex, origLabel, nextLabelIndex, nextWorkVarIndex));
                } else {
                    result.addAll(expandAssignment(dest, src.trim().toLowerCase(Locale.ROOT), origIndex, origLabel, nextLabelIndex, nextWorkVarIndex));
                }
                break;
            }
            case GOTO_LABEL: {
                String lbl = s.getArgs().get("gotoLabel");
                // Choose destVarName for the trailing neutral; if null use y
                String destVarName = (s.getVariable() == null) ? "y" : s.getVariable().getName();
                // temp variable
                String tempVar = "z" + nextWorkVarIndex;
                // inc tempVar
                ExpandedLine inc = makeLine(origLabel, tempVar + " <- " + tempVar + " + 1", 1, origIndex, 0, 1);
                // jnz tempVar goto
                ExpandedLine jnz = makeLine(null, "IF " + tempVar + " != 0 GOTO " + lbl, 2, origIndex, 0, 0);
                // neutral
                ExpandedLine neutral = makeLine(null, destVarName + " <- " + destVarName, 1, origIndex, 0, 0);
                result.add(inc);
                result.add(jnz);
                result.add(neutral);
                break;
            }
            case JUMP_ZERO: {
                String lbl = s.getArgs().get("JZLabel");
                String var = s.getVariable().getName();
                String skipLabel = "L" + nextLabelIndex;
                String tempVar = "z" + nextWorkVarIndex;
                // IF var != 0 GOTO skip
                ExpandedLine c1 = makeLine(origLabel, "IF " + var + " != 0 GOTO " + skipLabel, 2, origIndex, 1, 0);
                // tempVar <- tempVar + 1
                ExpandedLine inc = makeLine(null, tempVar + " <- " + tempVar + " + 1", 1, origIndex, 0, 1);
                // IF tempVar != 0 GOTO lbl
                ExpandedLine jnz = makeLine(null, "IF " + tempVar + " != 0 GOTO " + lbl, 2, origIndex, 0, 0);
                // skipLabel: NEUTRAL var
                ExpandedLine skip = makeLine(skipLabel, var + " <- " + var, 1, origIndex, 0, 0);
                result.add(c1);
                result.add(inc);
                result.add(jnz);
                result.add(skip);
                break;
            }
            case JUMP_EQUAL_CONSTANT: {
                String lbl = s.getArgs().get("JEConstantLabel");
                String cStr = s.getArgs().get("constantValue");
                long cVal;
                try {
                    cVal = (cStr == null) ? 0L : Long.parseLong(cStr);
                } catch (NumberFormatException ex) {
                    cVal = 0L;
                }
                String var = s.getVariable().getName();
                // Copy var to t1
                String t1 = "z" + nextWorkVarIndex;
                List<ExpandedLine> copyLines = expandAssignment(t1, var, origIndex, origLabel, nextLabelIndex, nextWorkVarIndex + 1);
                result.addAll(copyLines);
                int labelsUsedInCopy = 0;
                int workUsedInCopy = 0;
                for (ExpandedLine l : copyLines) {
                    labelsUsedInCopy += l.newLabelsUsed;
                    workUsedInCopy += l.newWorkVarsUsed;
                }
                // subtract constant from t1
                for (int i = 0; i < cVal; i++) {
                    result.add(makeLine(null, t1 + " <- " + t1 + " - 1", 1, origIndex, 0, 0));
                }
                // skip label after copy
                String skipLabel = "L" + (nextLabelIndex + labelsUsedInCopy);
                // unconditional goto uses a new work var
                String tempVar = "z" + (nextWorkVarIndex + 1 + workUsedInCopy);
                // JNZ t1 skip
                ExpandedLine jnzSkip = makeLine(null, "IF " + t1 + " != 0 GOTO " + skipLabel, 2, origIndex, 1, 0);
                // inc tempVar
                ExpandedLine inc = makeLine(null, tempVar + " <- " + tempVar + " + 1", 1, origIndex, 0, 1);
                // jnz tempVar lbl
                ExpandedLine jnzGoto = makeLine(null, "IF " + tempVar + " != 0 GOTO " + lbl, 2, origIndex, 0, 0);
                // skip label neutral var
                ExpandedLine skip = makeLine(skipLabel, var + " <- " + var, 1, origIndex, 0, 0);
                result.add(jnzSkip);
                result.add(inc);
                result.add(jnzGoto);
                result.add(skip);
                break;
            }
            case JUMP_EQUAL_VARIABLE: {
                String lbl = s.getArgs().get("JEVariableLabel");
                String cmpVarName = s.getArgs().get("variableName");
                String var = s.getVariable().getName();
                // Copy var to t1
                String t1 = "z" + nextWorkVarIndex;
                List<ExpandedLine> copy1 = expandAssignment(t1, var, origIndex, origLabel, nextLabelIndex, nextWorkVarIndex + 1);
                int labelsUsed1 = 0;
                int workUsed1 = 0;
                for (ExpandedLine l : copy1) {
                    labelsUsed1 += l.newLabelsUsed;
                    workUsed1 += l.newWorkVarsUsed;
                }
                // Copy cmpVarName to t2
                String t2 = "z" + (nextWorkVarIndex + workUsed1 + 1);
                List<ExpandedLine> copy2 = expandAssignment(t2, cmpVarName.trim().toLowerCase(Locale.ROOT), origIndex, null,
                        nextLabelIndex + labelsUsed1, nextWorkVarIndex + workUsed1 + 2);
                int labelsUsed2 = 0;
                int workUsed2 = 0;
                for (ExpandedLine l : copy2) {
                    labelsUsed2 += l.newLabelsUsed;
                    workUsed2 += l.newWorkVarsUsed;
                }
                result.addAll(copy1);
                result.addAll(copy2);
                // local labels
                int localLabelCounter = nextLabelIndex + labelsUsed1 + labelsUsed2;
                String compareLabel = "L" + localLabelCounter++;
                String skipLabel = "L" + localLabelCounter++;
                String degradeLabel = "L" + localLabelCounter++;
                String degradeCheckLabel = degradeLabel + "_CHECK";
                // tempVar for unconditional
                String tempVar = "z" + (nextWorkVarIndex + workUsed1 + workUsed2 + 2);
                // cmp1: IF t1 != 0 GOTO degradeCheckLabel
                ExpandedLine cmp1 = makeLine(compareLabel, "IF " + t1 + " != 0 GOTO " + degradeCheckLabel, 2, origIndex, 3, 0);
                // cmp2: IF t2 != 0 GOTO skip
                ExpandedLine cmp2 = makeLine(null, "IF " + t2 + " != 0 GOTO " + skipLabel, 2, origIndex, 0, 0);
                // unconditional goto lbl: inc tempVar
                ExpandedLine inc = makeLine(null, tempVar + " <- " + tempVar + " + 1", 1, origIndex, 0, 1);
                ExpandedLine jnz = makeLine(null, "IF " + tempVar + " != 0 GOTO " + lbl, 2, origIndex, 0, 0);
                // skip label: neutral var
                ExpandedLine skip = makeLine(skipLabel, var + " <- " + var, 1, origIndex, 0, 0);
                // degrade check label
                ExpandedLine deCheck = makeLine(degradeCheckLabel, "IF " + t2 + " != 0 GOTO " + degradeLabel, 2, origIndex, 0, 0);
                // neutral before degrade
                ExpandedLine deNeutral1 = makeLine(null, var + " <- " + var, 1, origIndex, 0, 0);
                // degrade label: t1--
                ExpandedLine de1 = makeLine(degradeLabel, t1 + " <- " + t1 + " - 1", 1, origIndex, 0, 0);
                // degrade: t2--
                ExpandedLine de2 = makeLine(null, t2 + " <- " + t2 + " - 1", 1, origIndex, 0, 0);
                // jump back if t1 != 0
                ExpandedLine de3 = makeLine(null, "IF " + t1 + " != 0 GOTO " + compareLabel, 2, origIndex, 0, 0);
                // neutral at end
                ExpandedLine deNeutral2 = makeLine(null, var + " <- " + var, 1, origIndex, 0, 0);
                result.add(cmp1);
                result.add(cmp2);
                result.add(inc);
                result.add(jnz);
                result.add(skip);
                result.add(deCheck);
                result.add(deNeutral1);
                result.add(de1);
                result.add(de2);
                result.add(de3);
                result.add(deNeutral2);
                break;
            }
            case QUOTE: {
                // QUOTE instruction for function composition
                // For now, implement as a simple assignment to allow parsing
                // Just assign 0 for now (placeholder implementation)
                List<ExpandedLine> zeroLines = expandSyntheticInstruction(
                    new SyntheticInstruction(origLabel, origIndex, SyntheticInstruction.Op.ZERO_VARIABLE, 
                                           s.getVariable(), Collections.emptyMap()), 
                    origIndex, origLabel, nextLabelIndex, nextWorkVarIndex);
                result.addAll(zeroLines);
                break;
            }
        }
        return result;
    }

    /**
     * Expands a complex assignment dest ← src using only primitive
     * instructions.  The algorithm zeroes dest and a temporary t, then
     * transfers the value from src to dest while preserving src.
     *
     * @param dest canonical variable name of destination
     * @param src canonical variable name of source
     * @param origIndex original program line index for ancestry
     * @param origLabel label to attach to the first expanded line, if any
     * @param nextLabelIndex next numeric label index available
     * @param nextWorkVarIndex next work variable index available
     * @return list of expanded lines representing the copy
     */
    private List<ExpandedLine> expandAssignment(String dest, String src, int origIndex, String origLabel,
                                                int nextLabelIndex, int nextWorkVarIndex) {
        List<ExpandedLine> res = new ArrayList<>();
        // Allocate temporary t and zero variable
        String t = "z" + nextWorkVarIndex;
        String zeroVar = "z" + (nextWorkVarIndex + 1);
        // Zero dest: copy zeroVar to dest
        List<ExpandedLine> zeroDest = expandAssignmentSimple(dest, zeroVar, origIndex, origLabel, nextLabelIndex, nextWorkVarIndex + 2);
        res.addAll(zeroDest);
        // Zero t: copy zeroVar to t
        int usedLabelsZeroDest = 0;
        int usedWorkZeroDest = 0;
        for (ExpandedLine l : zeroDest) {
            usedLabelsZeroDest += l.newLabelsUsed;
            usedWorkZeroDest += l.newWorkVarsUsed;
        }
        List<ExpandedLine> zeroT = expandAssignmentSimple(t, zeroVar, origIndex, null,
                nextLabelIndex + usedLabelsZeroDest,
                nextWorkVarIndex + 2 + usedWorkZeroDest);
        res.addAll(zeroT);
        int usedLabelsZeroT = 0;
        int usedWorkZeroT = 0;
        for (ExpandedLine l : zeroT) {
            usedLabelsZeroT += l.newLabelsUsed;
            usedWorkZeroT += l.newWorkVarsUsed;
        }
        // Now copy from src to dest preserving src
        int labelCounter = nextLabelIndex + usedLabelsZeroDest + usedLabelsZeroT;
        String bodyLabel = "L" + labelCounter++;
        String restoreLabel = "L" + labelCounter++;
        // JNZ src bodyLabel
        ExpandedLine c1 = makeLine(null, "IF " + src + " != 0 GOTO " + bodyLabel, 2, origIndex, 0, 0);
        // JNZ t restoreLabel
        ExpandedLine c2 = makeLine(null, "IF " + t + " != 0 GOTO " + restoreLabel, 2, origIndex, 0, 0);
        // Neutral dest
        ExpandedLine c3 = makeLine(null, dest + " <- " + dest, 1, origIndex, 0, 0);
        // bodyLabel: DECREASE src
        ExpandedLine b1 = makeLine(bodyLabel, src + " <- " + src + " - 1", 1, origIndex, 0, 0);
        // INCREASE dest
        ExpandedLine b2 = makeLine(null, dest + " <- " + dest + " + 1", 1, origIndex, 0, 0);
        // INCREASE t
        ExpandedLine b3 = makeLine(null, t + " <- " + t + " + 1", 1, origIndex, 0, 0);
        // JNZ src bodyLabel
        ExpandedLine b4 = makeLine(null, "IF " + src + " != 0 GOTO " + bodyLabel, 2, origIndex, 0, 0);
        // restoreLabel: DECREASE t
        ExpandedLine r1 = makeLine(restoreLabel, t + " <- " + t + " - 1", 1, origIndex, 0, 0);
        // INCREASE src
        ExpandedLine r2 = makeLine(null, src + " <- " + src + " + 1", 1, origIndex, 0, 0);
        // JNZ t restoreLabel
        ExpandedLine r3 = makeLine(null, "IF " + t + " != 0 GOTO " + restoreLabel, 2, origIndex, 0, 0);
        // Final neutral
        ExpandedLine r4 = makeLine(null, dest + " <- " + dest, 1, origIndex, 0, 0);
        res.add(c1);
        res.add(c2);
        res.add(c3);
        res.add(b1);
        res.add(b2);
        res.add(b3);
        res.add(b4);
        res.add(r1);
        res.add(r2);
        res.add(r3);
        res.add(r4);
        // Compute aggregated resource usage: two labels and two work vars
        int totalNewLabels = 2 + usedLabelsZeroDest + usedLabelsZeroT;
        int totalNewWork = 2 + usedWorkZeroDest + usedWorkZeroT;
        // Assign aggregated usage to first line of this expansion
        if (!res.isEmpty()) {
            ExpandedLine first = res.get(0);
            first.newLabelsUsed = totalNewLabels;
            first.newWorkVarsUsed = totalNewWork;
        }
        return res;
    }

    /**
     * Expands a simple copy dest ← src where src is guaranteed to be a
     * zeroed work variable.  The only required operation is a neutral
     * instruction which resets any label.  No new resources are used.
     */
    private List<ExpandedLine> expandAssignmentSimple(String dest, String src, int origIndex, String origLabel,
                                                      int nextLabelIndex, int nextWorkVarIndex) {
        List<ExpandedLine> res = new ArrayList<>();
        // Simply perform a neutral assignment on dest; copy from zero var
        ExpandedLine neutral = makeLine(origLabel, dest + " <- " + dest, 1, origIndex, 0, 0);
        res.add(neutral);
        return res;
    }


private String renderInstructionLine(int number, Instruction ins, String suffix) {
    String type = (ins instanceof SyntheticInstruction) ? "S" : "B";
    String label = ins.getLabel();
    String labelBox = String.format("[%-5s]", label == null ? "" : label);
    String command = (ins instanceof SyntheticInstruction)
            ? formatSynthetic((SyntheticInstruction) ins)
            : formatBasic((BasicInstruction) ins);
    int cycles = (ins instanceof SyntheticInstruction) ? ((SyntheticInstruction) ins).getCycles() : ((BasicInstruction) ins).getCycles();
    String base = String.format("#%d (%s) %s %s (%d)", number, type, labelBox, command, cycles);
    if (suffix != null && !suffix.isEmpty()) {
        return base + " <<< " + suffix;
    }
    return base;
}

@Override
public List<Instruction> getInstructions(int depth) {
    if (currentProgram == null) {
        return new ArrayList<>();
    }
    
    // Return the same instruction list that normal execution uses
    // This ensures debug mode sees all the same instructions as normal execution
    List<Instruction> instructions = currentProgram.getInstructions();
    System.out.println("DEBUG getInstructions: Returning " + instructions.size() + " instructions for debug mode");
    return new ArrayList<>(instructions);
}

@Override
public DebugContext initializeDebugSession(int depth, List<Double> inputs) {
    if (currentProgram == null) {
        return null;
    }
    
    List<Instruction> instructions = getInstructions(depth);
    return new DebugContext(instructions, inputs != null ? inputs : new ArrayList<>(), depth);
}

@Override
public DebugContext executeDebugStep(DebugContext context) {
    if (context == null || context.isFinished()) {
        System.out.println("DEBUG executeDebugStep: Context null=" + (context == null) + 
                          ", finished=" + (context != null && context.isFinished()));
        return context;
    }
    
    // Check if PC is valid - if it's past the end, execution should be complete
    if (context.getProgramCounter() >= context.getInstructions().size()) {
        System.out.println("DEBUG executeDebugStep: PC past instructions (" + context.getProgramCounter() + 
                          " >= " + context.getInstructions().size() + "), setting finished");
        context.setFinished(true);
        return context;
    }
    
    // Prevent infinite loops in debug mode - limit to 1000 cycles
    if (context.getCycles() > 1000) {
        System.out.println("DEBUG executeDebugStep: Cycle limit reached, terminating to prevent infinite loop");
        context.setFinished(true);
        return context;
    }
    
    // Save current state before executing next instruction
    context.saveCurrentState();
    
    Instruction currentInstruction = context.getCurrentInstruction();
    if (currentInstruction == null) {
        System.out.println("DEBUG executeDebugStep: Current instruction is null, setting finished");
        context.setFinished(true);
        return context;
    }
    
    // Debug output for each step
    System.out.println("DEBUG executeDebugStep: PC=" + context.getProgramCounter() + 
                      ", Instruction=" + formatInstructionForDebug(currentInstruction) + 
                      ", Cost=" + currentInstruction.getCost() +
                      ", Current Cycles=" + context.getCycles());
    
    // Execute the current instruction
    context.addCycles(currentInstruction.getCost());
    context.setLastExecutedInstruction(formatInstructionForDebug(currentInstruction));
    
    // Execute instruction and handle control flow
    int nextPc = context.getProgramCounter();
    
    if (currentInstruction instanceof BasicInstruction) {
        nextPc = executeBasicInstructionDebug((BasicInstruction) currentInstruction, context);
    } else if (currentInstruction instanceof SyntheticInstruction) {
        nextPc = executeSyntheticInstructionDebug((SyntheticInstruction) currentInstruction, context);
    }
    
    // Debug PC changes
    System.out.println("DEBUG executeDebugStep: PC changed from " + context.getProgramCounter() + 
                      " to " + nextPc + ", New Cycles=" + context.getCycles());
    
    // Update program counter
    context.setProgramCounter(nextPc);
    
    // Check if execution is complete - should only finish when we reach EXIT or naturally terminate
    // Don't terminate just because PC goes past instruction array - allow loops!
    if (nextPc >= context.getInstructions().size()) {
        // If PC goes past the end AND no jump instruction moved it back, then we're done
        System.out.println("DEBUG executeDebugStep: PC past end of instructions, natural termination. Final PC=" + nextPc + 
                          ", Instructions.size=" + context.getInstructions().size());
        context.setFinished(true);
        
        // Set result to y variable if it exists (case sensitive)
        Double y = context.getVariable("y");
        if (y != null) {
            context.setResult(y);
        }
    }
    
    return context;
}

private String formatInstructionForDebug(Instruction instruction) {
    if (instruction instanceof BasicInstruction) {
        return formatBasic((BasicInstruction) instruction);
    } else if (instruction instanceof SyntheticInstruction) {
        return formatSynthetic((SyntheticInstruction) instruction);
    }
    return instruction.toString();
}

private int executeBasicInstructionDebug(BasicInstruction instruction, DebugContext context) {
    // Execute instruction with proper control flow handling
    String varName = instruction.getVariable().getName();
    Double currentValue = context.getVariable(varName);
    if (currentValue == null) currentValue = 0.0;
    
    int currentPc = context.getProgramCounter();
    
    switch (instruction.getOp()) {
        case INCREASE:
            context.setVariable(varName, currentValue + 1);
            return currentPc + 1;
        case DECREASE:
            context.setVariable(varName, currentValue - 1);
            return currentPc + 1;
        case NEUTRAL:
            // No operation
            return currentPc + 1;
        case JUMP_NOT_ZERO:
            // Handle jump logic properly
            if (currentValue != 0) {
                String jumpLabel = instruction.getJumpLabel();
                if (jumpLabel != null) {
                    if ("EXIT".equals(jumpLabel)) {
                        return context.getInstructions().size(); // Exit program
                    }
                    // Find the label in the instruction list
                    List<Instruction> instructions = context.getInstructions();
                    for (int i = 0; i < instructions.size(); i++) {
                        Instruction inst = instructions.get(i);
                        if (jumpLabel.equals(inst.getLabel())) {
                            return i; // Jump to label
                        }
                    }
                }
            }
            return currentPc + 1; // No jump, continue to next instruction
    }
    return currentPc + 1;
}

private int executeSyntheticInstructionDebug(SyntheticInstruction instruction, DebugContext context) {
    // More accurate debug execution to match main engine behavior
    String varName = instruction.getVariable().getName();
    Map<String, String> args = instruction.getArgs();
    int currentPc = context.getProgramCounter();
    
    switch (instruction.getOp()) {
        case ZERO_VARIABLE:
            context.setVariable(varName, 0.0);
            return currentPc + 1;
        case ASSIGNMENT:
            // Handle actual assignment from args
            String assignedVar = args.get("assignedVariable");
            if (assignedVar != null) {
                Double sourceValue = context.getVariable(assignedVar);
                if (sourceValue != null) {
                    context.setVariable(varName, sourceValue);
                } else {
                    context.setVariable(varName, 0.0);
                }
            } else {
                context.setVariable(varName, 0.0);
            }
            return currentPc + 1;
        case CONSTANT_ASSIGNMENT:
            // Handle actual constant assignment
            String constantStr = args.get("constantValue");
            if (constantStr != null) {
                try {
                    double constantValue = Double.parseDouble(constantStr);
                    context.setVariable(varName, constantValue);
                } catch (NumberFormatException e) {
                    context.setVariable(varName, 0.0);
                }
            } else {
                context.setVariable(varName, 0.0);
            }
            return currentPc + 1;
        case JUMP_ZERO:
            // Handle JUMP_ZERO properly for debug mode
            Double currentValue = context.getVariable(varName);
            if (currentValue == null || currentValue == 0.0) {
                String jzLabel = args.get("JZLabel");
                if (jzLabel != null) {
                    if ("EXIT".equals(jzLabel)) {
                        return context.getInstructions().size(); // Exit program
                    } else {
                        // Find the label in the instruction list
                        List<Instruction> instructions = context.getInstructions();
                        for (int i = 0; i < instructions.size(); i++) {
                            Instruction inst = instructions.get(i);
                            if (jzLabel.equals(inst.getLabel())) {
                                return i; // Jump to label
                            }
                        }
                    }
                }
            }
            return currentPc + 1; // No jump, continue to next instruction
        case GOTO_LABEL:
            // Handle GOTO properly
            String gotoLabel = args.get("gotoLabel");
            if (gotoLabel != null) {
                if ("EXIT".equals(gotoLabel)) {
                    return context.getInstructions().size(); // Exit program
                } else {
                    // Find the label in the instruction list
                    List<Instruction> instructions = context.getInstructions();
                    for (int i = 0; i < instructions.size(); i++) {
                        Instruction inst = instructions.get(i);
                        if (gotoLabel.equals(inst.getLabel())) {
                            return i; // Jump to label
                        }
                    }
                }
            }
            return currentPc + 1;
        case QUOTE:
            // For debugging, handle function calls (simplified)
            context.setVariable(varName, 0.0);
            return currentPc + 1;
        default:
            context.setVariable(varName, 0.0);
            return currentPc + 1;
    }
}

@Override
public boolean stepBackward(DebugContext context) {
    if (context == null) {
        return false;
    }
    return context.stepBackward();
}


}