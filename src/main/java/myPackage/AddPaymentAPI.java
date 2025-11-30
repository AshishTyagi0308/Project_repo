package myPackage;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@SuppressWarnings("unchecked")
@WebServlet("/AddPaymentAPI")
public class AddPaymentAPI extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Ashish_mca@1234";

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:5173",
        "https://wellness-management-system.vercel.app",
        "https://admonitorial-cinderella-hungerly.ngrok-free.dev"
    );

    private void setCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin); 
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            response.setHeader("Access-Control-Allow-Origin", "null");
        }
        response.setHeader("Vary", "Origin");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "864000"); // cache preflight 10 days
    }

    // Helper for consistent error response
    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // Always BAD_REQUEST for frontend error
        response.setContentType("application/json");
        JSONObject err = new JSONObject();
        err.put("success", false);
        err.put("message", message);
        PrintWriter out = response.getWriter();
        out.print(err.toJSONString());
        out.flush();
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

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        setCorsHeaders(req, resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        setCorsHeaders(req, resp);

        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            if (!isTokenValid(token, resp)) {
                return;
            }
        }

        // Read JSON body from request
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JSONObject jsonRequest;
        try {
            JSONParser parser = new JSONParser();
            jsonRequest = (JSONObject) parser.parse(sb.toString());
        } catch (Exception e) {
            sendError(resp, "Invalid JSON input");
            return;
        }

        // Always extract id only from URL param "id"
        String id = req.getParameter("id");

        // Extract payment params from JSON
        String payMode = (String) jsonRequest.get("mode");
        String startDate = (String) jsonRequest.get("pay_date");
        String endDate = (String) jsonRequest.get("due_date"); // <-- can be ""

        String paidFeeStr = (String) jsonRequest.get("amount");

        // Validation: allow empty due_date by removing from required list
        if (id == null || id.isEmpty() ||
            payMode == null || payMode.isEmpty() ||
            startDate == null || startDate.isEmpty() ||
            paidFeeStr == null || paidFeeStr.isEmpty()) {
            sendError(resp, "Missing required payment parameters");
            return;
        }

        // Set due_date to null if empty string (will become SQL NULL)
        String dueDateForDB = (endDate == null || endDate.trim().isEmpty()) ? null : endDate;

        try {
            double paidFee = Double.parseDouble(paidFeeStr);
            Class.forName("com.mysql.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                String insertSQL = "INSERT INTO payment (Membership_ID, Pay_mode, Pay_date, Due_date, Paid_fee) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
                    stmt.setString(1, id);
                    stmt.setString(2, payMode);
                    stmt.setString(3, startDate);
                    // Allow due_date to be nullable
                    if (dueDateForDB == null) {
                        stmt.setNull(4, java.sql.Types.DATE);
                    } else {
                        stmt.setString(4, dueDateForDB);
                    }
                    stmt.setDouble(5, paidFee);

                    int rows = stmt.executeUpdate();
                    JSONObject result = new JSONObject();
                    if (rows > 0) {
                        result.put("success", true);
                        result.put("message", "Add Payment Successful.");
                        result.put("pay_mode", payMode);
                        result.put("pay_date", startDate);
                        result.put("due_date", dueDateForDB); // null/empty allowed
                        result.put("amount", paidFee);
                        resp.setContentType("application/json");
                        resp.getWriter().write(result.toJSONString());
                    } else {
                        sendError(resp, "Database insertion failed");
                    }
                }
            }
        } catch (NumberFormatException e) {
            sendError(resp, "Invalid paid_fee format");
        } catch (SQLException e) {
            sendError(resp, "Database error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            sendError(resp, "JDBC Driver not found");
        }
    }
}