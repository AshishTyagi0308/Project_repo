package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@SuppressWarnings("unchecked")
@WebServlet("/AddNewUserAPI")
public class AddNewUserAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Ashish_mca@1234";

    // List of allowed origins
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:5173",
        "https://wellness-management-system.vercel.app"
    );

    private void addCORSHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin); // Set single, valid origin
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "864000");
    }
    
    private boolean isTokenValid(String token, HttpServletResponse response) throws IOException {
        try {
            Jwts.parser()
                .setSigningKey("RaJdNoqNevTsnjh9Vgbe/LgPCrbcjwTCfKWpBuOyPTM=".getBytes())
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            sendError(response, "Invalid token signature");
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            sendError(response, "Token expired");
            return false;
        } catch (Exception e) {
            sendError(response, "Malformed or invalid token: " + e.getMessage());
            return false;
        }
    }

    // Helper method for sending JSON errors
    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject err = new JSONObject();
        err.put("error", message);
        out.print(err.toJSONString());
        out.flush();
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCORSHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCORSHeaders(request, response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        if (!isTokenValid(token, response)) {
            // Error sent inside isTokenValid; return directly
            return;
        }
        Gson gson = new Gson();
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();

        // Read incoming data
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        try {
            // Parse JSON
            JsonObject jsonObject = gson.fromJson(sb.toString(), JsonObject.class);
            String username = jsonObject.get("username").getAsString().trim();
            String password = jsonObject.get("password").getAsString().trim();
            String role = jsonObject.get("role").getAsString().trim().toLowerCase();
            String email = jsonObject.get("email").getAsString().trim();

            // Validate input
            if (username.isEmpty() || password.isEmpty() || role.isEmpty() || email.isEmpty()) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "All fields are required.");
                out.print(gson.toJson(jsonResponse));
                out.flush();
                return;
            }

            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO user (Username, Password, Role, Email) VALUES (?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, role);
                ps.setString(4, email);

                int rowsInserted = ps.executeUpdate();
                if (rowsInserted > 0) {
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Data inserted successfully.");
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Failed to insert data.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Internal server error: " + e.getMessage());
        }

        out.print(gson.toJson(jsonResponse));
        out.flush();
    }
}
