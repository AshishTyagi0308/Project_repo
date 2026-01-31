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
import org.json.simple.parser.ParseException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("unchecked")
@WebServlet("/EditPaymentAPI")
public class EditPaymentAPI extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Ashish_mca@1234";

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

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject err = new JSONObject();
        err.put("success", false);
        err.put("message", message);
        out.print(err.toJSONString());
        out.flush();
    }

    // Accept both yyyy-MM-dd and MM/dd/yyyy, always save as yyyy-MM-dd
    private String toMySQLDate(String dateStr) throws java.text.ParseException {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        Date date;
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd");
            iso.setLenient(false);
            date = iso.parse(dateStr);
        } catch (java.text.ParseException e) {
            SimpleDateFormat us = new SimpleDateFormat("MM/dd/yyyy");
            us.setLenient(false);
            date = us.parse(dateStr);
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    // Always output as MM/dd/yyyy for frontend
    private String toFrontendDate(String dbDateStr) throws java.text.ParseException {
        if (dbDateStr == null || dbDateStr.trim().isEmpty()) return "";
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(dbDateStr);
        return new SimpleDateFormat("MM/dd/yyyy").format(date);
    }
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // JWT check
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(resp, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp)) {
            return;
        }

        // id from URL/query param (Payment_ID)
        String id = req.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            sendError(resp, "Missing Payment ID in query parameter.");
            return;
        }

        // All other fields from JSON body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JSONObject params;
        try {
            JSONParser parser = new JSONParser();
            params = (JSONObject) parser.parse(sb.toString());
        } catch (ParseException e) {
            sendError(resp, "Malformed JSON payload");
            return;
        }

        String payMode   = params.get("mode")      != null ? params.get("mode").toString()      : "";
        String payDate   = params.get("pay_date")  != null ? params.get("pay_date").toString()  : "";
        String dueDate   = params.get("due_date")  != null ? params.get("due_date").toString()  : "";
        String paidFeeStr= params.get("amount")    != null ? params.get("amount").toString()    : "";

        // Validate required fields (except dueDate)
        if (payMode.trim().isEmpty() || payDate.trim().isEmpty() || paidFeeStr.trim().isEmpty()) {
            sendError(resp, "All fields are required except Due Date");
            return;
        }

        // Normalize dates to yyyy-MM-dd
        try {
            payDate = toMySQLDate(payDate);
            dueDate = (dueDate != null && !dueDate.trim().isEmpty()) ? toMySQLDate(dueDate) : null;
        } catch (java.text.ParseException pe) {
            sendError(resp, "Invalid date format. Use MM/dd/yyyy or yyyy-MM-dd.");
            return;
        }

        try {
            double paidFee = Double.parseDouble(paidFeeStr);
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {

                // 1) Get Membership_ID for this payment
                Integer membershipId = null;
                String findMembershipSql = "SELECT Membership_ID FROM payment WHERE Payment_ID = ?";
                try (PreparedStatement ps = conn.prepareStatement(findMembershipSql)) {
                    ps.setString(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            membershipId = rs.getInt("Membership_ID");
                        }
                    }
                }

                if (membershipId == null) {
                    sendError(resp, "Payment record not found for given id.");
                    return;
                }

                // 2) Fetch membership end date
                String membershipEndDateStr = null;
                String membershipSql = "SELECT End_date FROM membership WHERE Membership_ID = ?";
                try (PreparedStatement ms = conn.prepareStatement(membershipSql)) {
                    ms.setInt(1, membershipId);
                    try (ResultSet rs = ms.executeQuery()) {
                        if (rs.next()) {
                            Date endDateSql = rs.getDate("End_date");
                            if (endDateSql != null) {
                                membershipEndDateStr =
                                        new SimpleDateFormat("yyyy-MM-dd").format(endDateSql);
                            }
                        }
                    }
                }

                if (membershipEndDateStr == null) {
                    sendError(resp, "Membership end date not found for Membership_ID: " + membershipId);
                    return;
                }

                // 3) dueDate must be <= membershipEndDate
                if (dueDate != null && dueDate.compareTo(membershipEndDateStr) > 0) {
                    sendError(resp, "Payment due date cannot be after membership end date (" +
                            membershipEndDateStr + ")");
                    return;
                }

                // 4) Update payment
                String updateSQL =
                        "UPDATE payment SET Pay_mode = ?, Pay_date = ?, Due_date = ?, Paid_fee = ? WHERE Payment_ID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSQL)) {
                    stmt.setString(1, payMode);
                    stmt.setString(2, payDate);
                    if (dueDate == null) {
                        stmt.setNull(3, java.sql.Types.DATE);
                    } else {
                        stmt.setString(3, dueDate);
                    }
                    stmt.setDouble(4, paidFee);
                    stmt.setString(5, id);

                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        JSONObject result = new JSONObject();
                        result.put("success", true);
                        result.put("pay_mode", payMode);
                        result.put("pay_date", toFrontendDate(payDate));
                        result.put("due_date", (dueDate == null) ? "" : toFrontendDate(dueDate));
                        result.put("amount", paidFee);
                        resp.setContentType("application/json");
                        resp.getWriter().write(result.toJSONString());
                    } else {
                        sendError(resp, "Payment record not found for update");
                    }
                } catch (java.text.ParseException e) {
                    sendError(resp, "Server date conversion error");
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
