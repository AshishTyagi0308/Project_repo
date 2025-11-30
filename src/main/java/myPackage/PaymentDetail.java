package myPackage;

import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.sql.*;
import org.json.simple.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/PaymentDetail")
public class PaymentDetail extends HttpServlet {

    private static final String URL  = "jdbc:mysql://localhost:3306/gym";
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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        // Authentication
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(resp, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp)) {
            return;
        }

        String membershipId = req.getParameter("membership_id");
        JSONObject json = new JSONObject();

        if (membershipId == null || membershipId.trim().isEmpty()) {
            json.put("error", "membership_id is required");
            out.print(json.toString());
            return;
        }

        Connection con = null;
        PreparedStatement ps1 = null, ps2 = null, ps3 = null;
        ResultSet rs1 = null, rs2 = null, rs3 = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            // 1) Get total fee from membership table
            String q1 = "SELECT Total_fee FROM membership WHERE Membership_ID=?";
            ps1 = con.prepareStatement(q1);
            ps1.setString(1, membershipId);
            rs1 = ps1.executeQuery();

            double totalFee = 0;
            if (rs1.next()) {
                totalFee = rs1.getDouble("Total_fee");
            } else {
                json.put("error", "Invalid membership_id");
                out.print(json.toString());
                return;
            }

            // 2) Get total paid fee
            String q2 = "SELECT SUM(Paid_fee) AS paidFee FROM payment WHERE Membership_ID=?";
            ps2 = con.prepareStatement(q2);
            ps2.setString(1, membershipId);
            rs2 = ps2.executeQuery();

            double paidFee = 0;
            if (rs2.next()) {
                paidFee = rs2.getDouble("paidFee");
            }

            // 3) Get Due_date of the latest payment row
            //    "latest" defined by Pay_date, and then Payment_ID as tiebreaker
            String q3 = "SELECT Due_date " +
                        "FROM payment " +
                        "WHERE Membership_ID=? " +
                        "ORDER BY Pay_date DESC, Payment_ID DESC " +
                        "LIMIT 1";
            ps3 = con.prepareStatement(q3);
            ps3.setString(1, membershipId);
            rs3 = ps3.executeQuery();

            String dueDate = null;
            if (rs3.next()) {
                dueDate = rs3.getString("Due_date");
            }

            // If latest row has no due date, show "-"
            if (dueDate == null || dueDate.trim().isEmpty()) {
                dueDate = "-";
            }

            // 4) Pending fee calculation
            double pendingFee = totalFee - paidFee;
            if (pendingFee < 0) pendingFee = 0;
            String status = (pendingFee == 0) ? "Paid" : "Pending";

            // 5) Respond as JSON
            json.put("membership_id", membershipId);
            json.put("total_fee", totalFee);
            json.put("paid_fee", paidFee);
            json.put("pending_fee", pendingFee);
            json.put("status", status);
            json.put("due_date", dueDate);

            out.print(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            json.put("error", e.getMessage());
            out.print(json.toString());
        } finally {
            try { if (rs1 != null) rs1.close(); } catch (Exception ignored) {}
            try { if (rs2 != null) rs2.close(); } catch (Exception ignored) {}
            try { if (rs3 != null) rs3.close(); } catch (Exception ignored) {}
            try { if (ps1 != null) ps1.close(); } catch (Exception ignored) {}
            try { if (ps2 != null) ps2.close(); } catch (Exception ignored) {}
            try { if (ps3 != null) ps3.close(); } catch (Exception ignored) {}
            try { if (con != null) con.close(); } catch (Exception ignored) {}
        }
    }
}
