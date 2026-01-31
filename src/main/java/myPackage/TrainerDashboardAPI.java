package myPackage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/TrainerDashboardAPI")
public class TrainerDashboardAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        // Authentication: Check Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        if (!isTokenValid(token, response)) {
            return;
        }

        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String url = "jdbc:mysql://localhost:3306/gym";
        String user = "root";
        String pass = "Ashish_mca@1234";

        String query =
            "SELECT t.Trainer_ID AS id, " +
            "       t.Name AS name, " +
            "       t.Status AS status, " +
            "       t.Phone_no AS contact, " +
            "       COUNT(tm.Member_ID) AS assigned_members " +
            "FROM trainer t " +
            "LEFT JOIN trainer_member tm ON t.Trainer_ID = tm.Trainer_ID " +
            "GROUP BY t.Trainer_ID, t.Name, t.Status, t.Phone_no " +
            "ORDER BY t.Trainer_ID";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject err = new JSONObject();
            err.put("error", "MySQL JDBC Driver not found. " + e.getMessage());
            out.print(err.toJSONString());
            out.flush();
            return;
        }

        try (Connection con = DriverManager.getConnection(url, user, pass);
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            JSONArray resultList = new JSONArray();

            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getInt("id"));
                obj.put("name", rs.getString("name"));
                obj.put("assigned_members", rs.getInt("assigned_members"));
                obj.put("status", rs.getString("status"));
                obj.put("contact", rs.getString("contact"));
                resultList.add(obj);
            }

            out.print(resultList.toJSONString());

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject err = new JSONObject();
            err.put("error", "SQL Error: " + e.getMessage());
            out.print(err.toJSONString());
        } finally {
            out.flush();
        }
    }

    // Allow PUT with same JWT protection (for future updates if needed)
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }

        String token = authHeader.substring(7);

        if (!isTokenValid(token, response)) {
            return;
        }

        PrintWriter out = response.getWriter();
        JSONObject res = new JSONObject();
        res.put("message", "PUT access allowed for TrainerDashboardAPI (no update logic implemented yet)");
        out.print(res.toJSONString());
        out.flush();
    }
}
