package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/DeleteUserAPI")
public class DeleteUserAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // MySQL connection details
    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";
    
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:5173",
        "https://wellness-management-system.vercel.app",
        "https://admonitorial-cinderella-hungerly.ngrok-free.dev"
    );
    
    private void setCorsHeaders(HttpServletResponse response, HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        response.setHeader("Access-Control-Allow-Methods", "DELETE, OPTIONS");
        String reqHeaders = request.getHeader("Access-Control-Request-Headers");
        if (reqHeaders != null) {
            response.setHeader("Access-Control-Allow-Headers", reqHeaders);
        } else {
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        }
        response.setHeader("Access-Control-Max-Age", "86400");
    }
    
 // JWT validation with error reporting
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
        setCorsHeaders(response, request);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setCorsHeaders(response, request);
        response.setContentType("text/plain");
        
     // Authentication: Check Authorization header
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

        String username = request.getParameter("username");
        if (username == null || username.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Username is required");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                String selectSql = "SELECT * FROM user WHERE Username = ?";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, username);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            response.getWriter().write("User not found");
                            return;
                        }
                    }
                }
                String deleteSql = "DELETE FROM user WHERE Username = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, username);
                    int affected = deleteStmt.executeUpdate();
                    if (affected > 0) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getWriter().write("User deleted successfully");
                    } else {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        response.getWriter().write("Failed to delete user");
                    }
                }
            }
        } catch (Exception e) {
            setCorsHeaders(response, request);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error: " + e.getMessage());
        }
    }
}
