package se.emulator.server;

import se.emulator.engine.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Manages the server state including users, programs, functions, and execution history.
 * This class is thread-safe and handles all server-side operations.
 */
public class ServerManager {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, ProgramInfo> programs = new ConcurrentHashMap<>();
    private final Map<String, FunctionInfo> functions = new ConcurrentHashMap<>();
    private final Map<String, ExecutionHistory> executionHistory = new ConcurrentHashMap<>();
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private final EngineService engineService;
    
    // Store XML content for programs
    private final Map<String, String> programXmlContent = new ConcurrentHashMap<>();
    
 public ServerManager() {
    this.engineService = new EngineServiceImpl();

    // ✅ Add a default admin user with 1000 credits
    users.put("admin", new User("admin", 1000));
}

    
    // User management
    public synchronized User createUser(String username) {
        if (users.containsKey(username)) {
            return users.get(username);
        }
        
        User user = new User(username, 1000); // Start with 1000 credits
        users.put(username, user);
        return user;
    }
    
    public synchronized User getUser(String username) {
        return users.get(username);
    }
    
    public synchronized List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    // Program management
    public synchronized boolean uploadProgram(String username, String xmlContent) {
        try {
            // Create a temporary file to use with the existing engine
            File tempFile = File.createTempFile("program_", ".xml");
            tempFile.deleteOnExit();
            
            // Write XML content to temporary file
            Files.write(tempFile.toPath(), xmlContent.getBytes("UTF-8"));
            
            // Load the program using the engine
            LoadResult result = engineService.loadSystemFile(tempFile.getAbsolutePath());
            if (!result.isSuccess()) {
                return false;
            }
            
            String programName = engineService.getProgramName();
            if (programName == null) {
                return false;
            }
            
            // Check if program name already exists
            if (programs.containsKey(programName)) {
                return false;
            }
            
            // Get program details
            int maxExecutionLevel = engineService.getMaxExpansionDepth();
            String programDisplay = engineService.displayProgram();
            int instructionCount = 0;
            if (programDisplay != null) {
                instructionCount = programDisplay.split("\n").length;
            }
            
            // Create program info
            ProgramInfo programInfo = new ProgramInfo(
                programName, username, 
                instructionCount, 
                maxExecutionLevel
            );
            programs.put(programName, programInfo);
            programXmlContent.put(programName, xmlContent);
            
            // Update user statistics
            User user = users.get(username);
            if (user != null) {
                user.incrementMainPrograms();
            }
            
            // Add functions from the program
            List<Function> programFunctions = engineService.getFunctions();
            if (programFunctions != null) {
                for (Function function : programFunctions) {
                    String functionName = function.getName();
                    if (!functions.containsKey(functionName)) {
                        FunctionInfo functionInfo = new FunctionInfo(
                            functionName, function.getUserString(), programName, username,
                            function.getInstructions().size(), maxExecutionLevel
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
    
    public synchronized List<ProgramInfo> getAllPrograms() {
        return new ArrayList<>(programs.values());
    }
    
    public synchronized List<FunctionInfo> getAllFunctions() {
        return new ArrayList<>(functions.values());
    }
    
    // Execution management
    public synchronized ExecutionResult executeProgram(String username, String programName, 
                                                     Map<String, Long> inputs, int executionLevel) {
        User user = users.get(username);
        if (user == null) {
            return new ExecutionResult(false, "User not found", 0, 0, 0);
        }
        
        ProgramInfo programInfo = programs.get(programName);
        if (programInfo == null) {
            return new ExecutionResult(false, "Program not found", 0, 0, 0);
        }
        
        // Calculate cost based on architecture
        int cost = calculateExecutionCost(executionLevel);
        if (user.getCredits() < cost) {
            return new ExecutionResult(false, "Insufficient credits", 0, 0, 0);
        }
        
        try {
            // Load the program
            String xmlContent = programXmlContent.get(programName);
            if (xmlContent == null) {
                return new ExecutionResult(false, "Program content not found", 0, 0, 0);
            }
            
            // Create temporary file
            File tempFile = File.createTempFile("exec_", ".xml");
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), xmlContent.getBytes("UTF-8"));
            
            // Load program
            LoadResult loadResult = engineService.loadSystemFile(tempFile.getAbsolutePath());
            if (!loadResult.isSuccess()) {
                return new ExecutionResult(false, "Failed to load program", 0, 0, 0);
            }
            
            // Convert inputs to List<Double>
            List<Double> inputList = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                String varName = "x" + i;
                Long value = inputs.get(varName);
                inputList.add(value != null ? value.doubleValue() : 0.0);
            }
            
            // Execute program
            RunResult runResult = engineService.runProgram(executionLevel, inputList);
            if (runResult == null) {
                return new ExecutionResult(false, "Execution failed", 0, 0, 0);
            }
            
            // Deduct credits
            user.deductCredits(cost);
            
            // Record execution
            ExecutionHistory history = executionHistory.get(username);
            if (history == null) {
                history = new ExecutionHistory(username);
                executionHistory.put(username, history);
            }
            
            history.addExecution(programName, executionLevel, runResult.getY(), runResult.getCycles(), cost);
            
            // Update program statistics
            programInfo.incrementExecutionCount();
            programInfo.updateAverageCost(cost);
            
            return new ExecutionResult(true, "Execution successful", 
                                    runResult.getY(), runResult.getCycles(), cost);
            
        } catch (Exception e) {
            return new ExecutionResult(false, "Execution error: " + e.getMessage(), 0, 0, 0);
        }
    }
    
    private int calculateExecutionCost(int executionLevel) {
        // Simple cost calculation based on execution level
        switch (executionLevel) {
            case 1: return 5;   // Generation I
            case 2: return 10;  // Generation II
            case 3: return 20;  // Generation III
            case 4: return 40;  // Generation IV
            default: return 5;
        }
    }
    
    // Chat management
    public synchronized void addChatMessage(String username, String message) {
        ChatMessage chatMessage = new ChatMessage(username, message, System.currentTimeMillis());
        chatMessages.add(chatMessage);
        
        // Keep only last 100 messages
        if (chatMessages.size() > 100) {
            chatMessages.remove(0);
        }
    }
    
    public synchronized List<ChatMessage> getChatMessages() {
        return new ArrayList<>(chatMessages);
    }
    
    // Execution history
    public synchronized List<ExecutionHistory> getAllExecutionHistory() {
        return new ArrayList<>(executionHistory.values());
    }
    
    public synchronized ExecutionHistory getUserExecutionHistory(String username) {
        return executionHistory.get(username);
    }
    
    // Additional methods needed by servlets
    public synchronized boolean loginUser(String username) {
        createUser(username);
        return true;
    }
    
    public synchronized void rechargeUser(String username, long credits) {
        User user = users.get(username);
        if (user != null) {
            user.addCredits(credits);
        }
    }
    
    public synchronized ProgramInfo getProgram(String programName) {
        return programs.get(programName);
    }
    
    public synchronized FunctionInfo getFunction(String functionName) {
        return functions.get(functionName);
    }
    
    public synchronized List<ExecutionHistory> getUserHistory(String username) {
        ExecutionHistory history = executionHistory.get(username);
        if (history == null) {
            return new ArrayList<>();
        }
        return List.of(history);
    }
    
    // Singleton pattern
    private static ServerManager instance;
    
    public static synchronized ServerManager getInstance() {
        if (instance == null) {
            instance = new ServerManager();
        }
        return instance;
    }
}
