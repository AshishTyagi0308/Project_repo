package myPackage;
import org.json.simple.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked")
@WebServlet("/MemberDetail/*")
public class MemberDetail extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";
    
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:5173",
        "https://wellness-management-system.vercel.app",
        "https://admonitorial-cinderella-hungerly.ngrok-free.dev"
    );

    private void addCORSHeaders(HttpServletResponse response, HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        String reqHeaders = request.getHeader("Access-Control-Request-Headers");
        if (reqHeaders != null) {
            response.setHeader("Access-Control-Allow-Headers", reqHeaders);
        } else {
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        }
        response.setHeader("Access-Control-Max-Age", "864000");
    }

    // JWT validation with error reporting
    private boolean isTokenValid(String token, HttpServletResponse response, HttpServletRequest request) throws IOException {
        try {
            Jwts.parser()
                .setSigningKey("RaJdNoqNevTsnjh9Vgbe/LgPCrbcjwTCfKWpBuOyPTM=".getBytes())
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            sendError(response, request, "Invalid token signature");
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            sendError(response, request, "Token expired");
            return false;
        } catch (Exception e) {
            sendError(response, request, "Malformed or invalid token: " + e.getMessage());
            return false;
        }
    }

    // Helper method for sending JSON errors with CORS headers
    private void sendError(HttpServletResponse response, HttpServletRequest request, String message) throws IOException {
        addCORSHeaders(response, request);
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
        addCORSHeaders(response, request);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private Integer getMemberId(HttpServletRequest request) {
        String idParam = request.getParameter("id");
        if (idParam != null) {
            try { return Integer.parseInt(idParam); }
            catch (NumberFormatException e) { return null; }
        }
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            try { return Integer.parseInt(pathInfo.substring(1)); }
            catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCORSHeaders(response, request);  // Always set CORS headers!
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        // Authentication: Check Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, request, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        if (!isTokenValid(token, response, request)) {
            // Error sent inside isTokenValid; return directly
            return;
        }

        Integer memberId = getMemberId(request);
        if (memberId == null) {
            out.println("{\"error\":\"Invalid or missing member ID.\"}");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASS);

            String sql = "SELECT * FROM member WHERE Member_ID = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, memberId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                JSONObject member = new JSONObject();
                member.put("name", rs.getString("Name"));
                member.put("gender", rs.getString("Gender"));
                member.put("dob", rs.getString("DOB"));
                member.put("phone", rs.getString("Phone_no"));
                member.put("address", rs.getString("Address"));
                out.println(member.toJSONString());
            } else {
                out.println("{\"message\":\"Member not found.\"}");
            }
            con.close();
        } catch (Exception e) {
            addCORSHeaders(response, request); // Add CORS for error response, if not already added
            out.println("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
