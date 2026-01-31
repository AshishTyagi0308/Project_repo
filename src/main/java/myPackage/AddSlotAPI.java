package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/AddSlotAPI")
public class AddSlotAPI extends HttpServlet {

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
            sendAuthError(response, "Invalid token signature");
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            sendAuthError(response, "Token expired");
            return false;
        } catch (Exception e) {
            sendAuthError(response, "Malformed or invalid token: " + e.getMessage());
            return false;
        }
    }

    // 401-style error for auth
    private void sendAuthError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject err = new JSONObject();
        err.put("success", false);
        err.put("error", message);
        out.print(err.toJSONString());
        out.flush();
    }

    // Generic 4xx/5xx error helper
    private void sendErrorJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject err = new JSONObject();
        err.put("success", false);
        err.put("error", message);
        out.print(err.toJSONString());
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        // JWT check
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendAuthError(resp, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp)) {
            return;
        }

        // trainer_id as query param
        String trainerIdParam = req.getParameter("trainer_id");
        if (trainerIdParam == null || trainerIdParam.trim().isEmpty()) {
            sendErrorJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "trainer_id parameter is required.");
            return;
        }
        trainerIdParam = trainerIdParam.trim();

        // Read JSON body: { "label": "...", "start_time": "...", "end_time": "..." }
        StringBuilder jsonBody = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBody.append(line);
            }
        }

        String label;
        String startTime;
        String endTime;

        try {
            JSONParser parser = new JSONParser();
            JSONObject bodyObj = (JSONObject) parser.parse(jsonBody.toString());
            label = (String) bodyObj.get("label");
            startTime = (String) bodyObj.get("start_time");
            endTime = (String) bodyObj.get("end_time");
        } catch (Exception e) {
            sendErrorJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON body: " + e.getMessage());
            return;
        }

        if (label == null || startTime == null || endTime == null ||
        	    label.trim().isEmpty() || startTime.trim().isEmpty() || endTime.trim().isEmpty()) {

        	    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        	    resp.setContentType("application/json");
        	    JSONObject obj = new JSONObject();
        	    obj.put("success", false);
        	    obj.put("message", "All fields are required.");
        	    resp.getWriter().print(obj.toJSONString());
        	    return;
        	}


        Connection con = null;
        PreparedStatement insertSlotPs = null;
        PreparedStatement insertSchedulePs = null;
        ResultSet generatedKeys = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);
            con.setAutoCommit(false); // transaction

            // 1) Insert into slot_record
            String insertSlotSql =
                    "INSERT INTO slot_record (Start_time, End_time, Label) VALUES (?, ?, ?)";
            insertSlotPs = con.prepareStatement(insertSlotSql, Statement.RETURN_GENERATED_KEYS);
            insertSlotPs.setString(1, startTime.trim());
            insertSlotPs.setString(2, endTime.trim());
            insertSlotPs.setString(3, label.trim());

            int affected = insertSlotPs.executeUpdate();
            if (affected == 0) {
                con.rollback();
                sendErrorJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Failed to insert slot record.");
                return;
            }

            generatedKeys = insertSlotPs.getGeneratedKeys();
            if (!generatedKeys.next()) {
                con.rollback();
                sendErrorJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Failed to obtain generated Slot_ID.");
                return;
            }
            int slotId = generatedKeys.getInt(1);   // Slot_ID from slot_record

            // 2) Insert into trainer_schedule (Trainer_ID, Slot_ID; Schedule_ID auto)
            String insertScheduleSql =
                    "INSERT INTO trainer_schedule (Trainer_ID, Slot_ID) VALUES (?, ?)";
            insertSchedulePs = con.prepareStatement(insertScheduleSql, Statement.RETURN_GENERATED_KEYS);
            insertSchedulePs.setString(1, trainerIdParam);
            insertSchedulePs.setInt(2, slotId);
            int affectedSchedule = insertSchedulePs.executeUpdate();
            if (affectedSchedule == 0) {
                con.rollback();
                sendErrorJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Failed to insert trainer schedule.");
                return;
            }

            ResultSet scheduleKeys = insertSchedulePs.getGeneratedKeys();
            int scheduleId = 0;
            if (scheduleKeys.next()) {
                scheduleId = scheduleKeys.getInt(1);
            }
            scheduleKeys.close();

            con.commit();

            // Success response
            resp.setStatus(HttpServletResponse.SC_OK);
            JSONObject responseObj = new JSONObject();
            responseObj.put("success", true);
            responseObj.put("message", "Slot added successfully");
            responseObj.put("slot_id", slotId);
            responseObj.put("schedule_id", scheduleId);
            out.print(responseObj.toJSONString());
            out.flush();

        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
            sendErrorJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database insert failed: " + e.getMessage());
        } finally {
            try { if (generatedKeys != null) generatedKeys.close(); } catch (SQLException ignored) {}
            try { if (insertSchedulePs != null) insertSchedulePs.close(); } catch (SQLException ignored) {}
            try { if (insertSlotPs != null) insertSlotPs.close(); } catch (SQLException ignored) {}
            try { if (con != null) con.setAutoCommit(true); } catch (SQLException ignored) {}
            try { if (con != null) con.close(); } catch (SQLException ignored) {}
        }
    }
}
