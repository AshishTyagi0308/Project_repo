package myPackage;

import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/PaymentRecordFetch")
public class PaymentRecordFetch extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";
    
    // List of allowed origins for CORS
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:5173",
            "https://wellness-management-system.vercel.app",
            "https://admonitorial-cinderella-hungerly.ngrok-free.dev"
        );

    // Method to add CORS headers dynamically based on request Origin
    private void addCORSHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");

        // Check if the Origin header is in the allowed origins list
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            // Echo back the allowed origin to enable CORS
            response.setHeader("Access-Control-Allow-Origin", origin);
        }

        // Specify allowed HTTP methods
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        // Specify allowed headers clients can use in requests
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        // Set max age for preflight requests caching
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

    // Handle CORS preflight requests (OPTIONS)
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCORSHeaders(request, response);  // Set CORS headers dynamically
        response.setStatus(HttpServletResponse.SC_OK);
    }

    // Handle GET request to fetch payment records
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        addCORSHeaders(req, resp); // Add CORS headers in actual request as well

        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        
     // Authentication: Check Authorization header
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(resp, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        if (!isTokenValid(token, resp)) {
            // Error sent inside isTokenValid; return directly
            return;
        }

        String membershipId = req.getParameter("membership_id");

        if (membershipId == null || membershipId.trim().isEmpty()) {
            JSONObject error = new JSONObject();
            error.put("error", "membership_id is required");
            out.print(error.toString());
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            String query = "SELECT Payment_ID, Pay_date, Paid_fee, Pay_mode, due_date "
                         + "FROM payment WHERE Membership_ID=?";

            ps = con.prepareStatement(query);
            ps.setString(1, membershipId);
            rs = ps.executeQuery();

            JSONArray arr = new JSONArray();

            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("pay_id", rs.getInt("Payment_ID"));
                obj.put("pay_date", rs.getString("Pay_date"));
                obj.put("amount", rs.getDouble("Paid_fee"));
                obj.put("mode", rs.getString("Pay_mode"));
                obj.put("due_date", rs.getString("due_date"));   // Corrected key casing
                arr.add(obj);
            }

            if (arr.isEmpty()) {
                JSONObject noData = new JSONObject();
                noData.put("message", "No payment records found");
                out.print(noData.toString());
            } else {
                out.print(arr.toString());
            }

        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("error", e.getMessage());
            out.print(error.toString());
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            try { if (con != null) con.close(); } catch (Exception ignored) {}
        }
    }
}