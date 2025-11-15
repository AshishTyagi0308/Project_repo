package myPackage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/DeleteMemberAPI")
public class DeleteMemberAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // MySQL connection details
    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";
    
    // Add proper CORS headers dynamically based on origin
    private void setCorsHeaders(HttpServletResponse response, HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && (origin.contains("localhost") || origin.contains("ngrok-free.dev"))) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
        }
        response.setHeader("Access-Control-Allow-Methods", "DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "86400");
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }
    
    // Handle CORS preflight request
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setCorsHeaders(response, request);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    // Process DELETE request to delete user by username
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setCorsHeaders(response, request);
        response.setContentType("text/plain");

        String username = request.getParameter("username");
        if (username == null || username.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Username is required");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                // Check if the user exists
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

                // Delete the user
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
            setCorsHeaders(response, request); // Ensure CORS on error too
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error: " + e.getMessage());
        }
    }
}
