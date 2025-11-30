package myPackage;

import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.sql.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/PaymentRecordFetch")
public class PaymentRecordFetch extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

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

    // Handle GET request to fetch payment records
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

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

            String query = "SELECT Payment_ID, Pay_date, Paid_fee, Pay_mode, Due_date "
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

                String dueDate = rs.getString("Due_date");
                if (dueDate == null || dueDate.trim().isEmpty()) {
                    dueDate = "-";
                }
                obj.put("due_date", dueDate);

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
