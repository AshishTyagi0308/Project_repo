package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/SlotRecordAPI")
public class SlotRecordAPI extends HttpServlet {

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
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
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
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        // Auth header check
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(resp, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp)) {
            return;
        }

        // trainer_id from query param
        String trainerIdParam = req.getParameter("trainer_id");
        if (trainerIdParam == null || trainerIdParam.trim().isEmpty()) {
            sendError(resp, "trainer_id parameter is required.");
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            // Join trainer_schedule and slot_record
            String sql =
                "SELECT ts.Schedule_ID, sr.Start_time, sr.End_time, sr.Label " +
                "FROM trainer_schedule ts " +
                "JOIN slot_record sr ON ts.Slot_ID = sr.Slot_ID " +
                "WHERE ts.Trainer_ID = ? " +
                "ORDER BY sr.Start_time";

            ps = con.prepareStatement(sql);
            ps.setString(1, trainerIdParam);
            rs = ps.executeQuery();

            JSONArray slotsArray = new JSONArray();

            while (rs.next()) {
                JSONObject slot = new JSONObject();
                slot.put("schedule_id", rs.getInt("Schedule_ID"));
                slot.put("start_time", rs.getString("Start_time"));
                slot.put("end_time", rs.getString("End_time"));
                slot.put("label", rs.getString("Label"));
                slotsArray.add(slot);
            }

            out.print(slotsArray.toJSONString());
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
            sendError(resp, "Database query failed: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (ps != null) ps.close(); } catch (SQLException ignored) {}
            try { if (con != null) con.close(); } catch (SQLException ignored) {}
        }
    }
}
