package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
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

@WebServlet("/DeleteMembershipAPI")
public class DeleteMembershipAPI extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";  

    
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        // JWT authentication (optional, use if you need authentication for DELETE)
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

        // Read membership_id from request (URL param)
        String id = req.getParameter("id");

        JSONObject obj = new JSONObject();

        if (id == null) {
            obj.put("error", "membership_id is required");
            out.print(obj.toJSONString());
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            String query = "DELETE FROM membership WHERE Membership_ID = ?";

            ps = con.prepareStatement(query);
            ps.setString(1, id);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                obj.put("message", "Member deleted successfully");
                obj.put("membership_id", id);
            } else {
                obj.put("error", "No membership found with this ID");
            }

            out.print(obj.toJSONString());

        } catch (Exception e) {
            e.printStackTrace();
            obj.put("error", "Server error occurred");
            out.print(obj.toJSONString());
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
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

    // Improved Helper method to handle JSON error response
    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        JSONObject err = new JSONObject();
        err.put("error", message);
        PrintWriter out = response.getWriter();
        out.print(err.toJSONString());
        out.flush();
    }
}
