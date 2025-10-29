package se.emulator.server;

import se.emulator.engine.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Central manager for the S-Emulator server system.
 * Handles all users, programs, functions, and system state.
 */
public class ServerManager {
    private static final ServerManager instance = new ServerManager();
    
    // System state
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, ProgramInfo> programs = new ConcurrentHashMap<>();
    private final Map<String, FunctionInfo> functions = new ConcurrentHashMap<>();
    private final Map<String, List<ExecutionHistory>> userHistories = new ConcurrentHashMap<>();
    private final List<ChatMessage> chatMessages = new CopyOnWriteArrayList<>();
    
    // Engine for program execution
    private final EngineService engineService = new EngineServiceImpl();
    
    // Architecture definitions
    private static final Map<String, ArchitectureInfo> ARCHITECTURES = new HashMap<>();
    static {
        ARCHITECTURES.put("I", new ArchitectureInfo("I", 5, Arrays.asList(
            "NEUTRAL", "INCREASE", "DECREASE", "JUMP_NOT_ZERO"
        )));
        ARCHITECTURES.put("II", new ArchitectureInfo("II", 100, Arrays.asList(
            "NEUTRAL", "INCREASE", "DECREASE", "JUMP_NOT_ZERO",
            "ZERO_VARIABLE", "CONSTANT_ASSIGNMENT", "GOTO_LABEL"
        )));
        ARCHITECTURES.put("III", new ArchitectureInfo("III", 500, Arrays.asList(
            "NEUTRAL", "INCREASE", "DECREASE", "JUMP_NOT_ZERO",
            "ZERO_VARIABLE", "CONSTANT_ASSIGNMENT", "GOTO_LABEL",
            "ASSIGNMENT", "JUMP_ZERO", "JUMP_EQUAL_CONSTANT", "JUMP_EQUAL_VARIABLE"
        )));
        ARCHITECTURES.put("IV", new ArchitectureInfo("IV", 1000, Arrays.asList(
            "NEUTRAL", "INCREASE", "DECREASE", "JUMP_NOT_ZERO",
            "ZERO_VARIABLE", "CONSTANT_ASSIGNMENT", "GOTO_LABEL",
            "ASSIGNMENT", "JUMP_ZERO", "JUMP_EQUAL_CONSTANT", "JUMP_EQUAL_VARIABLE",
            "QUOTE", "JUMP_EQUAL_FUNCTION"
        )));
    }
    
    private ServerManager() {}
    
    public static ServerManager getInstance() {
        return instance;
    }
    
    // User management
    public synchronized boolean loginUser(String username) {
        if (users.containsKey(username)) {
            return false; // User already exists
        }
        users.put(username, new User(username, 1000)); // Start with 1000 credits
        userHistories.put(username, new CopyOnWriteArrayList<>());
        return true;
    }
    
    public User getUser(String username) {
        return users.get(username);
    }
    
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    public void rechargeUser(String username, long credits) {
        User user = users.get(username);
        if (user != null) {
            user.addCredits(credits);
        }
    }
    
    // Program management
    public synchronized boolean uploadProgram(String username, String xmlContent) {
        try {
            // Parse the XML content
            LoadResult result = engineService.loadSystemFileFromContent(xmlContent);
            if (!result.isSuccess()) {
                return false;
            }
            
            Program program = result.getProgram();
            String programName = program.getName();
            
            // Check if program name already exists
            if (programs.containsKey(programName)) {
                return false;
            }
            
            // Validate function references
            if (!validateFunctionReferences(program)) {
                return false;
            }
            
            // Create program info
            ProgramInfo programInfo = new ProgramInfo(
                programName, username, 
                program.getInstructions().size(), 
                program.getMaxExecutionLevel()
            );
            programs.put(programName, programInfo);
            
            // Update user statistics
            User user = users.get(username);
            if (user != null) {
                user.incrementMainPrograms();
            }
            
            // Add functions from the program
            if (program.getFunctions() != null) {
                for (Function function : program.getFunctions()) {
                    String functionName = function.getName();
                    if (!functions.containsKey(functionName)) {
                        FunctionInfo functionInfo = new FunctionInfo(
                            functionName, function.getUserString(), programName, username,
                            function.getInstructions().size(), function.getMaxExecutionLevel()
                        );
                        functions.put(functionName, functionInfo);
                        
                        if (user != null) {
                            user.incrementHelperFunctions();
                        }
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean validateFunctionReferences(Program program) {
        // Get all available function names
        Set<String> availableFunctions = new HashSet<>(functions.keySet());
        availableFunctions.addAll(Arrays.asList("CONST0", "Minus", "Successor", "Predecessor", "Add", 
            "AND", "NOT", "EQUAL", "Smaller_Equal_Than", "Smaller_Than"));
        
        // Check all instructions for function references
        for (Instruction instruction : program.getInstructions()) {
            if (instruction instanceof SyntheticInstruction) {
                SyntheticInstruction synth = (SyntheticInstruction) instruction;
                if (synth.getOp() == SyntheticInstruction.Op.QUOTE) {
                    String functionName = synth.getFunctionName();
                    if (!availableFunctions.contains(functionName)) {
                        return false;
                    }
                }
            }
        }
        
        // Check functions within the program
        if (program.getFunctions() != null) {
            for (Function function : program.getFunctions()) {
                for (Instruction instruction : function.getInstructions()) {
                    if (instruction instanceof SyntheticInstruction) {
                        SyntheticInstruction synth = (SyntheticInstruction) instruction;
                        if (synth.getOp() == SyntheticInstruction.Op.QUOTE) {
                            String functionName = synth.getFunctionName();
                            if (!availableFunctions.contains(functionName)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
    public List<ProgramInfo> getAllPrograms() {
        return new ArrayList<>(programs.values());
    }
    
    public List<FunctionInfo> getAllFunctions() {
        return new ArrayList<>(functions.values());
    }
    
    public ProgramInfo getProgram(String programName) {
        return programs.get(programName);
    }
    
    public FunctionInfo getFunction(String functionName) {
        return functions.get(functionName);
    }
    
    // Execution management
    public ExecutionResult executeProgram(String username, String programName, String architecture, 
                                        int executionLevel, Map<String, Long> inputs) {
        User user = users.get(username);
        if (user == null) {
            return new ExecutionResult(false, "User not found", 0, 0, 0);
        }
        
        ProgramInfo programInfo = programs.get(programName);
        if (programInfo == null) {
            return new ExecutionResult(false, "Program not found", 0, 0, 0);
        }
        
        ArchitectureInfo archInfo = ARCHITECTURES.get(architecture);
        if (archInfo == null) {
            return new ExecutionResult(false, "Invalid architecture", 0, 0, 0);
        }
        
        // Check if user has enough credits for architecture cost
        if (!user.deductCredits(archInfo.getCost())) {
            return new ExecutionResult(false, "Insufficient credits for architecture", 0, 0, 0);
        }
        
        try {
            // Load and execute the program
            LoadResult loadResult = engineService.loadSystemFileFromContent(getProgramXML(programName));
            if (!loadResult.isSuccess()) {
                user.addCredits(archInfo.getCost()); // Refund
                return new ExecutionResult(false, "Failed to load program", 0, 0, 0);
            }
            
            RunResult runResult = engineService.runProgram(loadResult.getProgram(), inputs, executionLevel);
            if (!runResult.isSuccess()) {
                user.addCredits(archInfo.getCost()); // Refund
                return new ExecutionResult(false, "Execution failed", 0, 0, 0);
            }
            
            // Calculate total cost (architecture cost + cycles)
            long totalCost = archInfo.getCost() + runResult.getCycles();
            if (!user.deductCredits(runResult.getCycles())) {
                // User ran out of credits during execution
                return new ExecutionResult(false, "Insufficient credits during execution", 
                    runResult.getCycles(), runResult.getFinalYValue(), totalCost);
            }
            
            // Record execution
            user.incrementExecutions();
            programInfo.recordExecution(totalCost);
            
            // Add to user history
            List<ExecutionHistory> history = userHistories.get(username);
            if (history != null) {
                history.add(new ExecutionHistory(
                    history.size() + 1, true, programName, architecture, executionLevel,
                    runResult.getFinalYValue(), runResult.getCycles(), totalCost
                ));
            }
            
            return new ExecutionResult(true, "Success", runResult.getCycles(), 
                runResult.getFinalYValue(), totalCost);
            
        } catch (Exception e) {
            user.addCredits(archInfo.getCost()); // Refund on error
            return new ExecutionResult(false, "Execution error: " + e.getMessage(), 0, 0, 0);
        }
    }
    
    private String getProgramXML(String programName) {
        // This would need to be implemented to retrieve the original XML content
        // For now, we'll assume the program is available in the engine
        return ""; // Placeholder
    }
    
    // Chat management
    public void addChatMessage(String sender, String message) {
        chatMessages.add(new ChatMessage(sender, message));
    }
    
    public List<ChatMessage> getChatMessages() {
        return new ArrayList<>(chatMessages);
    }
    
    // History management
    public List<ExecutionHistory> getUserHistory(String username) {
        return userHistories.getOrDefault(username, new ArrayList<>());
    }
    
    // Architecture information
    public Map<String, ArchitectureInfo> getArchitectures() {
        return new HashMap<>(ARCHITECTURES);
    }
    
    public ArchitectureInfo getArchitecture(String name) {
        return ARCHITECTURES.get(name);
    }
    
    // Helper classes
    public static class ArchitectureInfo {
        private final String name;
        private final long cost;
        private final List<String> supportedCommands;
        
        public ArchitectureInfo(String name, long cost, List<String> supportedCommands) {
            this.name = name;
            this.cost = cost;
            this.supportedCommands = supportedCommands;
        }
        
        public String getName() { return name; }
        public long getCost() { return cost; }
        public List<String> getSupportedCommands() { return supportedCommands; }
        
        public boolean supportsCommand(String command) {
            return supportedCommands.contains(command);
        }
    }
    
    public static class ExecutionResult {
        private final boolean success;
        private final String message;
        private final int cycles;
        private final long finalYValue;
        private final long totalCost;
        
        public ExecutionResult(boolean success, String message, int cycles, long finalYValue, long totalCost) {
            this.success = success;
            this.message = message;
            this.cycles = cycles;
            this.finalYValue = finalYValue;
            this.totalCost = totalCost;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getCycles() { return cycles; }
        public long getFinalYValue() { return finalYValue; }
        public long getTotalCost() { return totalCost; }
    }
}
