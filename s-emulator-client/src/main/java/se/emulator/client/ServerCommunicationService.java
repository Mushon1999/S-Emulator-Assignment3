package se.emulator.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import se.emulator.server.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Handles all communication with the S-Emulator server.
 */
public class ServerCommunicationService {
    private static final String BASE_URL = "http://localhost:8080/s-emulator-server/api";
    private final Gson gson = new Gson();
    
    // User operations
    public boolean loginUser(String username) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("username", username);
            
            String response = sendPostRequest("/user/login", gson.toJson(request));
            JsonObject result = gson.fromJson(response, JsonObject.class);
            return result.get("success").getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }
    
    public void rechargeUser(String username, long credits) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("username", username);
            request.addProperty("credits", credits);
            
            sendPostRequest("/user/recharge", gson.toJson(request));
        } catch (Exception e) {
            // Handle error silently
        }
    }
    
    public List<User> getAllUsers() {
        try {
            String response = sendGetRequest("/user");
            return gson.fromJson(response, new TypeToken<List<User>>(){}.getType());
        } catch (Exception e) {
            return List.of();
        }
    }
    
    // Program operations
    public boolean uploadProgram(String username, String xmlContent) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("username", username);
            request.addProperty("xmlContent", xmlContent);
            
            String response = sendPostRequest("/program/upload", gson.toJson(request));
            JsonObject result = gson.fromJson(response, JsonObject.class);
            return result.get("success").getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }
    
    public List<ProgramInfo> getAllPrograms() {
        try {
            String response = sendGetRequest("/program");
            return gson.fromJson(response, new TypeToken<List<ProgramInfo>>(){}.getType());
        } catch (Exception e) {
            return List.of();
        }
    }
    
    public ProgramInfo getProgram(String programName) {
        try {
            String response = sendGetRequest("/program/" + programName);
            return gson.fromJson(response, ProgramInfo.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    // Function operations
    public List<FunctionInfo> getAllFunctions() {
        try {
            String response = sendGetRequest("/function");
            return gson.fromJson(response, new TypeToken<List<FunctionInfo>>(){}.getType());
        } catch (Exception e) {
            return List.of();
        }
    }
    
    public FunctionInfo getFunction(String functionName) {
        try {
            String response = sendGetRequest("/function/" + functionName);
            return gson.fromJson(response, FunctionInfo.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    // Execution operations
    public ExecutionResult executeProgram(String username, String programName, 
                                        Map<String, Long> inputs, int executionLevel) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("username", username);
            request.addProperty("programName", programName);
            request.addProperty("executionLevel", executionLevel);
            
            JsonObject inputsJson = new JsonObject();
            for (Map.Entry<String, Long> entry : inputs.entrySet()) {
                inputsJson.addProperty(entry.getKey(), entry.getValue());
            }
            request.add("inputs", inputsJson);
            
            String response = sendPostRequest("/execution", gson.toJson(request));
            return gson.fromJson(response, ExecutionResult.class);
        } catch (Exception e) {
            return new ExecutionResult(false, "Communication error: " + e.getMessage(), 0, 0, 0);
        }
    }
    
    public List<ExecutionHistory> getUserExecutionHistory(String username) {
        try {
            String response = sendGetRequest("/execution/history/" + username);
            return gson.fromJson(response, new TypeToken<List<ExecutionHistory>>(){}.getType());
        } catch (Exception e) {
            return List.of();
        }
    }
    
    public Map<String, Object> getArchitectures() {
        try {
            String response = sendGetRequest("/execution/architectures");
            return gson.fromJson(response, new TypeToken<Map<String, Object>>(){}.getType());
        } catch (Exception e) {
            return Map.of();
        }
    }
    
    // Chat operations
    public void sendChatMessage(String username, String message) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("username", username);
            request.addProperty("message", message);
            
            sendPostRequest("/chat", gson.toJson(request));
        } catch (Exception e) {
            // Handle error silently
        }
    }
    
    public List<ChatMessage> getChatMessages() {
        try {
            String response = sendGetRequest("/chat");
            return gson.fromJson(response, new TypeToken<List<ChatMessage>>(){}.getType());
        } catch (Exception e) {
            return List.of();
        }
    }
    
    // Helper methods
    private String sendGetRequest(String endpoint) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } else {
            throw new Exception("HTTP error: " + responseCode);
        }
    }
    
    private String sendPostRequest(String endpoint, String jsonData) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonData.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } else {
            throw new Exception("HTTP error: " + responseCode);
        }
    }
}