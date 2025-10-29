package se.emulator.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Servlet for handling user-related operations.
 */
public class UserServlet extends HttpServlet {
    private final ServerManager serverManager = ServerManager.getInstance();
    private final Gson gson = new Gson();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Get all users
                List<User> users = serverManager.getAllUsers();
                out.print(gson.toJson(users));
            } else if (pathInfo.startsWith("/")) {
                // Get specific user
                String username = pathInfo.substring(1);
                User user = serverManager.getUser(username);
                if (user != null) {
                    out.print(gson.toJson(user));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\":\"User not found\"}");
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Login user
                JsonObject json = gson.fromJson(request.getReader(), JsonObject.class);
                String username = json.get("username").getAsString();
                
                boolean success = serverManager.loginUser(username);
                if (success) {
                    out.print("{\"success\":true,\"message\":\"Login successful\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    out.print("{\"success\":false,\"message\":\"Username already exists\"}");
                }
            } else if (pathInfo.equals("/recharge")) {
                // Recharge user credits
                JsonObject json = gson.fromJson(request.getReader(), JsonObject.class);
                String username = json.get("username").getAsString();
                long credits = json.get("credits").getAsLong();
                
                serverManager.rechargeUser(username, credits);
                out.print("{\"success\":true,\"message\":\"Credits recharged\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
