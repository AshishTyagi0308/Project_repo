package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/DeleteProgressAPI")
public class DeleteProgressAPI extends HttpServlet {

    private static final String URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    
    // JWT validation
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
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        JSONObject err = new JSONObject();
        err.put("error", message);
        PrintWriter out = response.getWriter();
        out.print(err.toJSONString());
        out.flush();
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        JSONObject obj = new JSONObject();

        // 1. JWT
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(resp, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp)) return;

        // 2. Progress_ID from query param ?id=
        String progressIdParam = req.getParameter("id");
        if (progressIdParam == null || progressIdParam.trim().isEmpty()) {
            sendError(resp, "Progress_ID (id param) is required");
            return;
        }

        int progressId;
        try {
            progressId = Integer.parseInt(progressIdParam);
        } catch (NumberFormatException e) {
            sendError(resp, "Progress_ID must be a valid integer");
            return;
        }

        // 3. JDBC delete
        Connection con = null;
        PreparedStatement ps = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            String sql = "DELETE FROM progress_chart WHERE Progress_ID = ?";
            ps = con.prepareStatement(sql);
            ps.setInt(1, progressId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                obj.put("success", true);
                obj.put("message", "Progress record deleted successfully");
                obj.put("progress_id", progressId);
                out.print(obj.toJSONString());
                out.flush();
            } else {
                sendError(resp, "No progress record found with this Progress_ID");
            }

        } catch (Exception e) {
            sendError(resp, "Database/server error: " + e.getMessage());
        } finally {
            try { if (ps  != null) ps.close();  } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }
}
