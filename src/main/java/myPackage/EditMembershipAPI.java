package myPackage;

import java.io.BufferedReader;
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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/EditMembershipAPI")
public class EditMembershipAPI extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:5173",
        "https://wellness-management-system.vercel.app",
        "https://admonitorial-cinderella-hungerly.ngrok-free.dev"
    );

    private void addCORSHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "864000");
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCORSHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        addCORSHeaders(req, resp);
        handleMembershipUpdate(req, resp);
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

    // Improved Helper method to handle JSON error response
    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        JSONObject err = new JSONObject();
        err.put("error", message);
        PrintWriter out = response.getWriter();
        out.print(err.toJSONString());
        out.flush();
    }

    private void handleMembershipUpdate(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        JSONObject obj = new JSONObject();

        PrintWriter out = resp.getWriter();
        addCORSHeaders(req, resp);

        // JWT Authentication
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(resp, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp)) return;

        // Get membershipId from param
        String membershipId = req.getParameter("id");
        if (membershipId == null || membershipId.trim().isEmpty()) {
            sendError(resp, "membership_id (id param) is required");
            return;
        }

        // Parse JSON body
        String type = null, startDate = null, endDate = null, amount = null;

        try {
            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String body = sb.toString();
            if (body.isEmpty()) {
                sendError(resp, "Request body is empty. Data required in JSON format.");
                return;
            }
            JSONObject json = (JSONObject) new JSONParser().parse(body);
            type = (String) json.get("type");
            startDate = (String) json.get("start_date");
            endDate = (String) json.get("end_date");
            amount = (String) json.get("amount");
        } catch (Exception e) {
            sendError(resp, "Failed to parse JSON: " + e.getMessage());
            return;
        }

        // Validation
        if (type == null || (!type.equalsIgnoreCase("PT") && !type.equalsIgnoreCase("GENERAL"))) {
            sendError(resp, "Invalid plan_type. Allowed: PT or GENERAL");
            return;
        }
        if (startDate == null || endDate == null || amount == null) {
            sendError(resp, "Missing one or more required fields: start_date, end_date, amount");
            return;
        }

        // JDBC
        Connection con = null;
        PreparedStatement ps = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Updated driver for MySQL 8+
            con = DriverManager.getConnection(URL, USER, PASS);

            String query = "UPDATE membership SET Membership_type = ?, Start_date = ?, End_date = ?, Total_fee = ? WHERE Membership_ID = ?";
            ps = con.prepareStatement(query);
            ps.setString(1, type);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            ps.setString(4, amount);
            ps.setString(5, membershipId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                obj.put("success", true);
                obj.put("message", "Member updated successfully");
                obj.put("membership_id", membershipId);
                obj.put("plan_type", type);
                obj.put("start_date", startDate);
                obj.put("end_date", endDate);
                obj.put("amount", amount);
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                sendError(resp, "No membership found with this ID");
                return;
            }
            out.print(obj.toJSONString());
        } catch (Exception e) {
            sendError(resp, "Database/server error: " + e.getMessage());
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }
}