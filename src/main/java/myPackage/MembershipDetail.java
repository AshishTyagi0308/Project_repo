package myPackage;
import java.io.PrintWriter;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

import org.json.simple.JSONArray;

@SuppressWarnings("unchecked")
@WebServlet("/MembershipDetail")
public class MembershipDetail extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";
    
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:5173",
            "https://wellness-management-system.vercel.app",
            "https://admonitorial-cinderella-hungerly.ngrok-free.dev"
        );

    // Helper method to add CORS headers
    private void addCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "864000");
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

        addCORSHeaders(resp);
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
        JSONArray membershipArray = new JSONArray();

        String memberId = req.getParameter("id");

        if (memberId == null || memberId.trim().equals("")) {
            JSONObject error = new JSONObject();
            error.put("error", "id is required");
            out.print(error.toString());
            out.flush();
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement pst = con.prepareStatement(
                         "SELECT Member_ID, Membership_ID, Membership_type, Start_date, End_date, Total_fee FROM membership WHERE Member_ID = ?")) {
                pst.setString(1, memberId);

                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        JSONObject obj = new JSONObject();
                        obj.put("id", rs.getString("Membership_ID"));
                        obj.put("type", rs.getString("Membership_type"));
                        obj.put("start_date", rs.getString("Start_date"));
                        obj.put("end_date", rs.getString("End_date"));
                        obj.put("fees", rs.getString("Total_fee"));
                        membershipArray.add(obj);
                    }
                }
            }

            out.print(membershipArray.toString());
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
            JSONObject error = new JSONObject();
            error.put("error", "Server Error");
            out.print(error.toString());
            out.flush();
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        addCORSHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
