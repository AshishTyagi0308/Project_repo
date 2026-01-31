package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.json.simple.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@SuppressWarnings("unchecked")
@WebServlet("/DeleteTrainerAPI")
public class DeleteTrainerAPI extends HttpServlet {

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
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject err = new JSONObject();
        err.put("success", false);
        err.put("error", message);
        out.print(err.toJSONString());
        out.flush();
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        // Auth like DeleteMemberAPI
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, response)) {
            return;
        }

        String trainerID = request.getParameter("id");
        if (trainerID == null || trainerID.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSONObject obj = new JSONObject();
            obj.put("success", false);
            obj.put("error", "id is required");
            response.getWriter().print(obj.toJSONString());
            return;
        }
        trainerID = trainerID.trim();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {

                // Check existence first (same style as DeleteMemberAPI)
                String selectSql = "SELECT * FROM trainer WHERE Trainer_ID = ?";
                try (PreparedStatement selectStmt = con.prepareStatement(selectSql)) {
                    selectStmt.setString(1, trainerID);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            JSONObject obj = new JSONObject();
                            obj.put("success", false);
                            obj.put("error", "Trainer not found");
                            response.getWriter().print(obj.toJSONString());
                            return;
                        }
                    }
                }

                String deleteSql = "DELETE FROM trainer WHERE Trainer_ID = ?";
                try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
                    ps.setString(1, trainerID);
                    int affected = ps.executeUpdate();

                    JSONObject obj = new JSONObject();
                    if (affected > 0) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        obj.put("success", true);
                        obj.put("message", "Trainer deleted successfully");
                    } else {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        obj.put("success", false);
                        obj.put("error", "Failed to delete trainer");
                    }
                    response.getWriter().print(obj.toJSONString());
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject obj = new JSONObject();
            obj.put("success", false);
            obj.put("error", "Error: " + e.getMessage());
            response.getWriter().print(obj.toJSONString());
        }
    }
}
