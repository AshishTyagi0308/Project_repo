package myPackage;

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

@WebServlet("/EditUserAPI")
public class EditUserAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Database connection parameters - change as per your setup
    private static final String DB_URL = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Ashish_mca@1234";

    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCorsHeaders(request, response);
        // For preflight requests, respond with status 200 OK.
        response.setStatus(HttpServletResponse.SC_OK);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCorsHeaders(request, response);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String role = request.getParameter("role");
        String email = request.getParameter("email");

        if (username == null || password == null || role == null || email == null || 
            username.isEmpty() || password.isEmpty() || role.isEmpty() || email.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Missing or empty parameters\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Connect to the database
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // Prepare SQL update query
            String sql = "UPDATE user SET Password = ?, Role = ?, Email = ? WHERE Username = ?";
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, password);
            stmt.setString(2, role);
            stmt.setString(3, email);
            stmt.setString(4, username);

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                out.print("{\"message\":\"User updated successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\":\"User not found\"}");
            }

        } catch (ClassNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"JDBC Driver not found\"}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Database error: " + e.getMessage() + "\"}");
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                // Ignore close errors
            }
        }
    }

    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        // Allow all origins. Change "*" to your frontend origin in production
        response.setHeader("Access-Control-Allow-Origin", "*");
        // Allow the specified HTTP methods
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        // Allow headers sent by client including content-type for POST payload
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        // Allow credentials if needed
        response.setHeader("Access-Control-Allow-Credentials", "true");
        // Cache preflight response for 3600 seconds (1 hour)
        response.setHeader("Access-Control-Max-Age", "3600");
    }
}
