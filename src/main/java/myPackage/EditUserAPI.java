package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// You need Gson for JSON parsing: Add dependency or jar to your project
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet("/EditUserAPI")
public class EditUserAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Ashish_mca@1234";

    // For all requests, add CORS headers
    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With,ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");
    }

    // Respond to preflight
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCorsHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    // Accept POST (optional, keep if frontend uses POST)
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCorsHeaders(request, response);
        // Can implement as before, or direct to doPut logic
    }

    // **This is the main fix: add PUT support**
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCorsHeaders(request, response);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String oldUsername = request.getParameter("oldUsername"); // from query string

        // Parse JSON body
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String body = sb.toString();

        Gson gson = new Gson();
        JsonObject jsonData = gson.fromJson(body, JsonObject.class);

        String username = jsonData.get("username").getAsString();
        String password = jsonData.get("password").getAsString();
        String role = jsonData.get("role").getAsString();
        String email = jsonData.get("email").getAsString();

        if (username == null || password == null || role == null || email == null ||
            username.isEmpty() || password.isEmpty() || role.isEmpty() || email.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Missing or empty parameters\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // Use oldUsername for WHERE clause if present, else use new username
            String sql = "UPDATE user SET Username=?, Password=?, Role=?, Email=? WHERE Username=?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.setString(4, email);
            stmt.setString(5, oldUsername != null && !oldUsername.isEmpty() ? oldUsername : username);

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                out.print("{\"success\":true,\"message\":\"User updated successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"success\":false,\"error\":\"User not found\"}");
            }

        } catch (ClassNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"error\":\"JDBC Driver not found\"}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"error\":\"Database error: " + e.getMessage() + "\"}");
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {}
        }
    }
}