package myPackage;

import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import org.json.simple.JSONObject;

@WebServlet("/PaymentDetail")
public class PaymentDetail extends HttpServlet {

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:5173",
        "https://wellness-management-system.vercel.app",
        "https://admonitorial-cinderella-hungerly.ngrok-free.dev"
    );

    // Sets CORS headers (simple: use "*" for dev, customize for production security)
    private void addCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*"); // For dev, use allowed origins in prod
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "86400");
    }

    // Handle CORS preflight
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCORSHeaders(response);  // <- Set CORS here!
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        addCORSHeaders(resp); // <- Set CORS here too!

        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        String membershipId = req.getParameter("membership_id");
        JSONObject json = new JSONObject();

        if (membershipId == null || membershipId.trim().isEmpty()) {
            json.put("error", "membership_id is required");
            out.print(json.toString());
            return;
        }

        Connection con = null;
        PreparedStatement ps1 = null, ps2 = null;
        ResultSet rs1 = null, rs2 = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            // 1️⃣ Get total fee from membership table
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

            // 2️⃣ Get paid and due date
            String q2 = "SELECT SUM(Paid_fee) AS paidFee, MAX(Due_date) AS Due_date FROM payment WHERE Membership_ID=?";
            ps2 = con.prepareStatement(q2);
            ps2.setString(1, membershipId);
            rs2 = ps2.executeQuery();

            double paidFee = 0;
            String dueDate = null;
            if (rs2.next()) {
                paidFee = rs2.getDouble("paidFee");
                dueDate = rs2.getString("Due_date");
            }

            // 3️⃣ Pending fee calculation
            double pendingFee = totalFee - paidFee;
            if (pendingFee < 0) pendingFee = 0;
            String status = (pendingFee == 0) ? "Paid" : "Pending";

            // 4️⃣ Respond as JSON
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
            try { if (ps1 != null) ps1.close(); } catch (Exception ignored) {}
            try { if (ps2 != null) ps2.close(); } catch (Exception ignored) {}
            try { if (con != null) con.close(); } catch (Exception ignored) {}
        }
    }
}