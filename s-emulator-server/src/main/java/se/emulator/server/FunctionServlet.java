package se.emulator.server;

import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Servlet for handling function-related operations.
 */
public class FunctionServlet extends HttpServlet {
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
                // Get all functions
                List<FunctionInfo> functions = serverManager.getAllFunctions();
                out.print(gson.toJson(functions));
            } else if (pathInfo.startsWith("/")) {
                // Get specific function
                String functionName = pathInfo.substring(1);
                FunctionInfo function = serverManager.getFunction(functionName);
                if (function != null) {
                    out.print(gson.toJson(function));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\":\"Function not found\"}");
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
