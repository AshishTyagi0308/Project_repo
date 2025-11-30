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

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:5173",
        "https://wellness-management-system.vercel.app",
        "https://admonitorial-cinderella-hungerly.ngrok-free.dev"
    );

    private void addCORSHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "86400");
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

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject err = new JSONObject();
        err.put("error", message);
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
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        addCORSHeaders(req, resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        addCORSHeaders(req, resp);
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.getWriter().print("{\"error\":\"POST not supported, use PUT\"}");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        addCORSHeaders(req, resp);

        // Check JWT token
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(resp, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp)) {
            return;
        }

        String id = req.getParameter("id");
        String payMode = req.getParameter("mode");
        String payDate = req.getParameter("pay_date");
        String dueDate = req.getParameter("due_date");
        String paidFeeStr = req.getParameter("amount");

        // For PUT and JS clients, read JSON from body if parameters missing
        if (id == null || payMode == null || payDate == null || dueDate == null || paidFeeStr == null) {
            try {
                BufferedReader reader = req.getReader();
                JSONParser parser = new JSONParser();
                JSONObject params = (JSONObject) parser.parse(reader);

                id = req.getParameter("id");
                if (id == null && params.containsKey("id")) {
                    id = String.valueOf(params.get("id"));
                }
                payMode = String.valueOf(params.getOrDefault("mode", ""));
                payDate = String.valueOf(params.getOrDefault("pay_date", ""));
                dueDate = String.valueOf(params.getOrDefault("due_date", ""));
                paidFeeStr = String.valueOf(params.getOrDefault("amount", ""));
            } catch (ParseException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\":\"Malformed JSON payload\"}");
                return;
            }
        }

        // Validate required fields (except dueDate)
        if (id == null || id.trim().isEmpty() ||
            payMode == null || payMode.trim().isEmpty() ||
            payDate == null || payDate.trim().isEmpty() ||
            paidFeeStr == null || paidFeeStr.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Missing required payment parameters\"}");
            return;
        }

        // Accept yyyy-MM-dd or MM/dd/yyyy, store as yyyy-MM-dd
        try {
            payDate = toMySQLDate(payDate);
            dueDate = (dueDate != null && !dueDate.trim().isEmpty()) ? toMySQLDate(dueDate) : null;
        } catch (java.text.ParseException pe) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Invalid date format. Use MM/dd/yyyy or yyyy-MM-dd.\"}");
            return;
        }

        try {
            double paidFee = Double.parseDouble(paidFeeStr);
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                String updateSQL = "UPDATE payment SET Pay_mode = ?, Pay_date = ?, Due_date = ?, Paid_fee = ? WHERE Payment_ID = ?";
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
                    JSONObject result = new JSONObject();
                    if (rows > 0) {
                        // Output dates as MM/dd/yyyy
                        result.put("success", true);
                        result.put("pay_mode", payMode);
                        result.put("pay_date", toFrontendDate(payDate));
                        result.put("due_date", dueDate == null ? "" : toFrontendDate(dueDate));
                        result.put("amount", paidFee);
                        resp.setContentType("application/json");
                        resp.getWriter().write(result.toJSONString());
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("{\"error\":\"Payment record not found for update\"}");
                    }
                } catch (java.text.ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Invalid amount format\"}");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Database error: " + e.getMessage() + "\"}");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
