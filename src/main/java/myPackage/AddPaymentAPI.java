package myPackage;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

        private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (!isTokenValid(token, resp)) {
                return;
            }
        }

        // Read JSON body
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

        // Parameters
        String id = req.getParameter("id");        // Membership_ID from URL
        String payMode = (String) jsonRequest.get("mode");
        String startDateStr = (String) jsonRequest.get("pay_date");
        String dueDateStr   = (String) jsonRequest.get("due_date");
        String paidFeeStr   = (String) jsonRequest.get("amount");

        if (id == null || id.isEmpty() ||
            payMode == null || payMode.isEmpty() ||
            startDateStr == null || startDateStr.isEmpty() ||
            paidFeeStr == null || paidFeeStr.isEmpty()) {
            sendError(resp, "All fields are required except Due Date");
            return;
        }

        // Parse dates
        LocalDate payDate;
        LocalDate dueDate = null;  // nullable
        try {
            payDate = LocalDate.parse(startDateStr);  // yyyy-MM-dd
            if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
                dueDate = LocalDate.parse(dueDateStr);
            }
        } catch (DateTimeParseException e) {
            sendError(resp, "Invalid date format. Expected yyyy-MM-dd");
            return;
        }

        try {
            double paidFee = Double.parseDouble(paidFeeStr);
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {

                // 1) Fetch membership end date for given Membership_ID
                LocalDate membershipEndDate = null;
                String membershipSql = "SELECT End_date FROM membership WHERE Membership_ID = ?";
                try (PreparedStatement ms = conn.prepareStatement(membershipSql)) {
                    ms.setString(1, id);
                    try (ResultSet rs = ms.executeQuery()) {
                        if (rs.next()) {
                            Date endDateSql = rs.getDate("End_date");
                            if (endDateSql != null) {
                                membershipEndDate = endDateSql.toLocalDate();
                            }
                        }
                    }
                }

                if (membershipEndDate == null) {
                    sendError(resp, "Membership not found or end date not set for Membership_ID: " + id);
                    return;
                }

                // 2) If dueDate is provided, it must be <= membershipEndDate
                if (dueDate != null && dueDate.isAfter(membershipEndDate)) {
                    sendError(resp, "Payment due date cannot be after membership end date (" 
                                      + membershipEndDate.toString() + ")");
                    return; // stop, do not insert
                }

                // 3) Insert payment
                String insertSQL =
                    "INSERT INTO payment (Membership_ID, Pay_mode, Pay_date, Due_date, Paid_fee) " +
                    "VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
                    stmt.setString(1, id);
                    stmt.setString(2, payMode);
                    stmt.setDate(3, Date.valueOf(payDate));

                    if (dueDate == null) {
                        stmt.setNull(4, java.sql.Types.DATE);
                    } else {
                        stmt.setDate(4, Date.valueOf(dueDate));
                    }

                    stmt.setDouble(5, paidFee);

                    int rows = stmt.executeUpdate();
                    JSONObject result = new JSONObject();
                    if (rows > 0) {
                        result.put("success", true);
                        result.put("message", "Add Payment Successful.");
                        result.put("pay_mode", payMode);
                        result.put("pay_date", payDate.toString());
                        result.put("due_date", (dueDate == null) ? null : dueDate.toString());
                        result.put("amount", paidFee);
                        resp.setContentType("application/json");
                        resp.getWriter().write(result.toJSONString());
                    } else {
                        sendError(resp, "Database insertion failed");
                    }
                }
            }
        } catch (NumberFormatException e) {
            sendError(resp, "Invalid amount format");
        } catch (SQLException e) {
            sendError(resp, "Database error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            sendError(resp, "JDBC Driver not found");
        }
    }
}
