package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/EditMembershipAPI")
public class EditMembershipAPI extends HttpServlet {

    private static final String URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleMembershipUpdate(req, resp);
    }

    // JWT validation with error reporting (same JSON style as success/fail)
    private boolean isTokenValid(String token, HttpServletResponse response) throws IOException {
        JSONObject obj = new JSONObject();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            Jwts.parser()
                .setSigningKey("RaJdNoqNevTsnjh9Vgbe/LgPCrbcjwTCfKWpBuOyPTM=".getBytes())
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            obj.put("success", false);
            obj.put("message", "Invalid token signature");
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            obj.put("success", false);
            obj.put("message", "Token expired");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            obj.put("success", false);
            obj.put("message", "Malformed or invalid token: " + e.getMessage());
        }

        out.print(obj.toJSONString());
        out.flush();
        return false;
    }

    private void handleMembershipUpdate(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        JSONObject obj = new JSONObject();

        // 1. JWT Authentication
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            obj.put("success", false);
            obj.put("message", "Unauthorized: Missing or invalid Authorization header.");
            out.print(obj.toJSONString());
            out.flush();
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp)) {
            return; // response already sent
        }

        // 2. Get membershipId from param
        String membershipId = req.getParameter("id");
        if (membershipId == null || membershipId.trim().isEmpty()) {
            obj.put("success", false);
            obj.put("message", "Membership ID (id param) is required.");
            out.print(obj.toJSONString());
            out.flush();
            return;
        }
        membershipId = membershipId.trim();

        // 3. Parse JSON body
        String type = null, startDate = null, endDate = null, amountStr = null;

        try {
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = req.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            String body = sb.toString();
            if (body.isEmpty()) {
                obj.put("success", false);
                obj.put("message", "Request body is empty. Data required in JSON format.");
                out.print(obj.toJSONString());
                out.flush();
                return;
            }

            JSONObject json = (JSONObject) new JSONParser().parse(body);
            type      = json.get("type")       != null ? ((String) json.get("type")).trim()        : "";
            startDate = json.get("start_date") != null ? ((String) json.get("start_date")).trim() : "";
            endDate   = json.get("end_date")   != null ? ((String) json.get("end_date")).trim()   : "";
            amountStr = json.get("amount")     != null ? ((String) json.get("amount")).trim()     : "";
        } catch (Exception e) {
            obj.put("success", false);
            obj.put("message", "Failed to parse JSON: " + e.getMessage());
            out.print(obj.toJSONString());
            out.flush();
            return;
        }

        // 4. Validation – same style as AddMembershipAPI
        if (type.isEmpty() || startDate.isEmpty() || endDate.isEmpty() || amountStr.isEmpty()) {
            obj.put("success", false);
            obj.put("message", "All fields are required.");
            out.print(obj.toJSONString());
            out.flush();
            return;
        }

        if (!type.equalsIgnoreCase("PT") && !type.equalsIgnoreCase("GENERAL")) {
            obj.put("success", false);
            obj.put("message", "Invalid plan_type. Allowed: PT or GENERAL.");
            out.print(obj.toJSONString());
            out.flush();
            return;
        }

        // Validate amount as decimal to avoid “Incorrect decimal value” from MySQL
        java.math.BigDecimal amount;
        try {
            amount = new java.math.BigDecimal(amountStr);
        } catch (NumberFormatException ex) {
            obj.put("success", false);
            obj.put("message", "Amount must be a valid number.");
            out.print(obj.toJSONString());
            out.flush();
            return;
        }

        // 5. JDBC update
        Connection con = null;
        PreparedStatement ps = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            String query = "UPDATE membership " +
                           "SET Membership_type = ?, Start_date = ?, End_date = ?, Total_fee = ? " +
                           "WHERE Membership_ID = ?";
            ps = con.prepareStatement(query);
            ps.setString(1, type);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            ps.setBigDecimal(4, amount);
            ps.setString(5, membershipId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                obj.put("success", true);
                obj.put("message", "Membership updated successfully.");
                obj.put("membership_id", membershipId);
                obj.put("plan_type", type);
                obj.put("start_date", startDate);
                obj.put("end_date", endDate);
                obj.put("amount", amount.toPlainString());
            } else {
                obj.put("success", false);
                obj.put("message", "No membership found with this ID.");
            }

        } catch (ClassNotFoundException | SQLException e) {
            obj.put("success", false);
            obj.put("message", "Database/server error: " + e.getMessage());
        } finally {
            try { if (ps  != null) ps.close();  } catch (Exception ignored) {}
            try { if (con != null) con.close(); } catch (Exception ignored) {}
        }

        out.print(obj.toJSONString());
        out.flush();
    }
}
