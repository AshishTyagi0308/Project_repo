package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/DeleteSlotAPI")
public class DeleteSlotAPI extends HttpServlet {

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
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        // JWT check
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(resp, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp)) {
            return;
        }

        // schedule_id from query param
        String scheduleIdParam = req.getParameter("schedule_id");
        if (scheduleIdParam == null || scheduleIdParam.trim().isEmpty()) {
            sendError(resp, "schedule_id parameter is required.");
            return;
        }

        Connection con = null;
        PreparedStatement findSlotPs = null;
        PreparedStatement deleteSchedulePs = null;
        PreparedStatement deleteSlotPs = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);
            con.setAutoCommit(false); // transaction so both deletes happen together

            // 1) Find Slot_ID from trainer_schedule
            String findSlotSql =
                    "SELECT Slot_ID FROM trainer_schedule WHERE Schedule_ID = ?";
            findSlotPs = con.prepareStatement(findSlotSql);
            findSlotPs.setString(1, scheduleIdParam);
            rs = findSlotPs.executeQuery();

            if (!rs.next()) {
                con.rollback();
                sendError(resp, "No schedule found for given schedule_id.");
                return;
            }
            int slotId = rs.getInt("Slot_ID");
            rs.close();

            // 2) Delete from trainer_schedule (child referencing schedule_id)
            String deleteScheduleSql =
                    "DELETE FROM trainer_schedule WHERE Schedule_ID = ?";
            deleteSchedulePs = con.prepareStatement(deleteScheduleSql);
            deleteSchedulePs.setString(1, scheduleIdParam);
            int deletedSchedule = deleteSchedulePs.executeUpdate();
            if (deletedSchedule == 0) {
                con.rollback();
                sendError(resp, "Delete failed in trainer_schedule.");
                return;
            }

            // 3) Delete from slot_record (the actual slot definition)
            String deleteSlotSql =
                    "DELETE FROM slot_record WHERE Slot_ID = ?";
            deleteSlotPs = con.prepareStatement(deleteSlotSql);
            deleteSlotPs.setInt(1, slotId);
            deleteSlotPs.executeUpdate();

            con.commit();

            JSONObject res = new JSONObject();
            res.put("message", "Slot deleted successfully");
            res.put("schedule_id", Integer.parseInt(scheduleIdParam));
            res.put("slot_id", slotId);
            out.print(res.toJSONString());
            out.flush();

        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
            sendError(resp, "Database delete failed: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (findSlotPs != null) findSlotPs.close(); } catch (SQLException ignored) {}
            try { if (deleteSchedulePs != null) deleteSchedulePs.close(); } catch (SQLException ignored) {}
            try { if (deleteSlotPs != null) deleteSlotPs.close(); } catch (SQLException ignored) {}
            try { if (con != null) con.setAutoCommit(true); } catch (SQLException ignored) {}
            try { if (con != null) con.close(); } catch (SQLException ignored) {}
        }
    }
}
