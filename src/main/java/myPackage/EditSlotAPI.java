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

@WebServlet("/EditSlotAPI")
public class EditSlotAPI extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    // normalize: if value is null or blank, return "-"
    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        return value.trim();
    }

    // JWT validation (same style as EditWorkoutAPI)
    private boolean isTokenValid(String token,
                                 HttpServletResponse response,
                                 HttpServletRequest request) throws IOException {
        try {
            Jwts.parser()
                .setSigningKey("RaJdNoqNevTsnjh9Vgbe/LgPCrbcjwTCfKWpBuOyPTM=".getBytes())
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            sendJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                     false, "Invalid token signature");
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            sendJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                     false, "Token expired");
            return false;
        } catch (Exception e) {
            sendJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                     false, "Malformed or invalid token: " + e.getMessage());
            return false;
        }
    }

    // common JSON sender: {"success":..., "message": "..."}
    private void sendJson(HttpServletResponse response,
                          int statusCode,
                          boolean success,
                          String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JSONObject obj = new JSONObject();
        obj.put("success", success);
        obj.put("message", message);
        PrintWriter out = response.getWriter();
        out.write(obj.toJSONString());
        out.flush();
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // if you call PUT from frontend, reuse logic
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // JWT check
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJson(resp, HttpServletResponse.SC_UNAUTHORIZED,
                     false, "Unauthorized: Missing or invalid Authorization header");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp, req)) {
            return;
        }

        // schedule_id from query param
        String scheduleIdParam = req.getParameter("schedule_id");
        if (scheduleIdParam == null || scheduleIdParam.trim().isEmpty()) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                     false, "schedule_id parameter is required");
            return;
        }

        // Read JSON body: { "label": "...", "start_time": "...", "end_time": "..." }
        StringBuilder jsonBody = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBody.append(line);
            }
        }

        if (jsonBody.length() == 0) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                     false, "Empty JSON body");
            return;
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
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                     false, "Invalid JSON body: " + e.getMessage());
            return;
        }

        // normalize null/empty values to "-"
        label = normalize(label);
        startTime = normalize(startTime);
        endTime = normalize(endTime);

        Connection con = null;
        PreparedStatement findSlotPs = null;
        PreparedStatement updateSlotPs = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            // 1) Find Slot_ID from trainer_schedule using Schedule_ID
            String findSlotSql =
                    "SELECT Slot_ID FROM trainer_schedule WHERE Schedule_ID = ?";
            findSlotPs = con.prepareStatement(findSlotSql);
            findSlotPs.setString(1, scheduleIdParam);
            rs = findSlotPs.executeQuery();

            if (!rs.next()) {
                sendJson(resp, HttpServletResponse.SC_NOT_FOUND,
                         false, "No schedule found for given schedule_id");
                return;
            }

            int slotId = rs.getInt("Slot_ID");

            // 2) Update slot_record using that Slot_ID
            String updateSql =
                    "UPDATE slot_record " +
                    "SET Start_time = ?, End_time = ?, Label = ? " +
                    "WHERE Slot_ID = ?";
            updateSlotPs = con.prepareStatement(updateSql);
            updateSlotPs.setString(1, startTime);
            updateSlotPs.setString(2, endTime);
            updateSlotPs.setString(3, label);
            updateSlotPs.setInt(4, slotId);

            int updated = updateSlotPs.executeUpdate();
            if (updated == 0) {
                sendJson(resp, HttpServletResponse.SC_NOT_FOUND,
                         false, "Update failed: slot not found");
                return;
            }

            // success response
            JSONObject responseObj = new JSONObject();
            responseObj.put("success", true);
            responseObj.put("message", "Slot updated successfully");
            responseObj.put("schedule_id", Integer.parseInt(scheduleIdParam));
            responseObj.put("slot_id", slotId);
            sendJsonOk(resp, responseObj);

        } catch (ClassNotFoundException e) {
            sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                     false, "JDBC driver not found");
        } catch (SQLException e) {
            sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                     false, ("Database update failed: " + e.getMessage()).replace("\"", "'"));
        } catch (Exception e) {
            sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                     false, ("Unexpected server error: " + e.getMessage()).replace("\"", "'"));
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (findSlotPs != null) findSlotPs.close(); } catch (SQLException ignored) {}
            try { if (updateSlotPs != null) updateSlotPs.close(); } catch (SQLException ignored) {}
            try { if (con != null) con.close(); } catch (SQLException ignored) {}
        }
    }

    // helper to send full success object
    private void sendJsonOk(HttpServletResponse response, JSONObject obj) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.write(obj.toJSONString());
        out.flush();
    }
}
