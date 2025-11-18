package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.json.simple.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/DeleteMemberAPI")
public class DeleteMemberAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;

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

    //authenticating token
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
        err.put("success", false);
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
        response.setContentType("application/json");

        // Authentication: Check Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, response)) {
            return; // JWT error already handled
        }

        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSONObject obj = new JSONObject();
            obj.put("success", false);
            obj.put("error", "id is required");
            response.getWriter().print(obj.toJSONString());
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                // Check existence
                String selectSql = "SELECT * FROM member WHERE Member_ID = ?";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, id);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            JSONObject obj = new JSONObject();
                            obj.put("success", false);
                            obj.put("error", "Member not found");
                            response.getWriter().print(obj.toJSONString());
                            return;
                        }
                    }
                }
                String deleteSql = "DELETE FROM member WHERE Member_ID = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, id);
                    int affected = deleteStmt.executeUpdate();
                    JSONObject obj = new JSONObject();
                    if (affected > 0) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        obj.put("success", true);
                        obj.put("message", "Member deleted successfully");
                    } else {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        obj.put("success", false);
                        obj.put("error", "Failed to delete member");
                    }
                    response.getWriter().print(obj.toJSONString());
                }
            }
        } catch (Exception e) {
            setCorsHeaders(response, request);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject obj = new JSONObject();
            obj.put("success", false);
            obj.put("error", "Error: " + e.getMessage());
            response.getWriter().print(obj.toJSONString());
        }
    }
}