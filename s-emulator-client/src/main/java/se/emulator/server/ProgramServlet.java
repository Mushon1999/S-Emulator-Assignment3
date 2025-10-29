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
 * Servlet for handling program-related operations.
 */
public class ProgramServlet extends HttpServlet {
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
            System.out.println("[ProgramServlet] GET request received at: " + pathInfo);

            if (pathInfo == null || pathInfo.equals("/")) {
                List<ProgramInfo> programs = serverManager.getAllPrograms();
                out.print(gson.toJson(programs));
            } else if (pathInfo.startsWith("/")) {
                String programName = pathInfo.substring(1);
                ProgramInfo program = serverManager.getProgram(programName);
                if (program != null) {
                    out.print(gson.toJson(program));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\":\"Program not found\"}");
                }
            }

            System.out.println("[ProgramServlet] GET completed successfully.");

        } catch (Exception e) {
            System.out.println("[ProgramServlet] Exception in GET: " + e.getMessage());
            e.printStackTrace(System.out);
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
            System.out.println("[ProgramServlet] POST request received at: " + pathInfo);

            if (pathInfo == null || pathInfo.equals("/")) {
                JsonObject json = gson.fromJson(request.getReader(), JsonObject.class);
                System.out.println("[ProgramServlet] JSON received: " + json);

                String username = json.get("username").getAsString();
                String xmlContent = json.get("xmlContent").getAsString();

                boolean success = serverManager.uploadProgram(username, xmlContent);

                if (success) {
                    System.out.println("[ProgramServlet] Upload success for user: " + username);
                    out.print("{\"success\":true,\"message\":\"Program uploaded successfully\"}");
                } else {
                    System.out.println("[ProgramServlet] Upload failed — invalid or duplicate program.");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\":false,\"message\":\"Invalid program or program name already exists\"}");
                }
            } else {
                System.out.println("[ProgramServlet] Invalid path for POST: " + pathInfo);
            }

        } catch (Exception e) {
            System.out.println("[ProgramServlet] Exception in POST: " + e.getMessage());
            e.printStackTrace(System.out);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}

