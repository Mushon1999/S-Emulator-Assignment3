package se.emulator.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet for handling program execution operations.
 */
public class ExecutionServlet extends HttpServlet {
    private final ServerManager serverManager = ServerManager.getInstance();
    private final Gson gson = new Gson();
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Execute program
                JsonObject json = gson.fromJson(request.getReader(), JsonObject.class);
                String username = json.get("username").getAsString();
                String programName = json.get("programName").getAsString();
                String architecture = json.get("architecture").getAsString();
                int executionLevel = json.get("executionLevel").getAsInt();
                
                // Parse inputs
                Map<String, Long> inputs = new HashMap<>();
                if (json.has("inputs")) {
                    JsonObject inputsJson = json.getAsJsonObject("inputs");
                    for (String key : inputsJson.keySet()) {
                        inputs.put(key, inputsJson.get(key).getAsLong());
                    }
                }
                
                ServerManager.ExecutionResult result = serverManager.executeProgram(
                    username, programName, architecture, executionLevel, inputs);
                
                if (result.isSuccess()) {
                    out.print(gson.toJson(result));
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\":false,\"message\":\"" + result.getMessage() + "\"}");
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();
        
        try {
            if (pathInfo != null && pathInfo.startsWith("/history/")) {
                // Get user execution history
                String username = pathInfo.substring(9); // Remove "/history/"
                List<ExecutionHistory> history = serverManager.getUserHistory(username);
                out.print(gson.toJson(history));
            } else if (pathInfo != null && pathInfo.equals("/architectures")) {
                // Get available architectures
                Map<String, ServerManager.ArchitectureInfo> architectures = serverManager.getArchitectures();
                out.print(gson.toJson(architectures));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
