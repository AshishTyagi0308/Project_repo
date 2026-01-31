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

@WebServlet("/EditWorkoutAPI")
public class EditWorkoutAPI extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Ashish_mca@1234";

    private boolean isTokenValid(String token,
                                 HttpServletResponse response,
                                 HttpServletRequest request) throws IOException {
        try {
            Jwts.parser()
                .setSigningKey("RaJdNoqNevTsnjh9Vgbe/LgPCrbcjwTCfKWpBuOyPTM=".getBytes())
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            sendJson(response, 401, false, "Invalid token signature");
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            sendJson(response, 401, false, "Token expired");
            return false;
        } catch (Exception e) {
            sendJson(response, 401, false, "Malformed or invalid token: " + e.getMessage());
            return false;
        }
    }

    // Optional fields can be null
    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private void sendJson(HttpServletResponse response,
                          int statusCode,
                          boolean success,
                          String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        JSONObject obj = new JSONObject();
        obj.put("success", success);
        obj.put("message", message);
        response.getWriter().write(obj.toJSONString());
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJson(response, 401, false, "Unauthorized: Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        if (!isTokenValid(token, response, request)) {
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            sendJson(response, 400, false, "Missing id parameter");
            return;
        }

        // BODY READER
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        if (sb.length() == 0) {
            sendJson(response, 400, false, "Empty JSON body");
            return;
        }

        String fromDate, toDate;
        String monday, tuesday, wednesday, thursday, friday, saturday, sunday;

        try {
            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse(sb.toString());

            fromDate = (String) body.get("start_date");
            toDate   = (String) body.get("end_date");

            // ❗ FINAL FIX — DATE REQUIRED CHECK (now works correctly)
            if (fromDate == null || fromDate.trim().isEmpty() ||
                toDate == null || toDate.trim().isEmpty()) {

                sendJson(response, 400, false, "Dates are required");
                return;
            }

            // Optional fields
            monday    = normalize((String) body.get("monday"));
            tuesday   = normalize((String) body.get("tuesday"));
            wednesday = normalize((String) body.get("wednesday"));
            thursday  = normalize((String) body.get("thursday"));
            friday    = normalize((String) body.get("friday"));
            saturday  = normalize((String) body.get("saturday"));
            sunday    = normalize((String) body.get("sunday"));

        } catch (Exception e) {
            sendJson(response, 400, false, "JSON parse error: " + e.getMessage());
            return;
        }

        String sql =
            "UPDATE workout_chart SET " +
            "Start_date = ?, End_date = ?, " +
            "Monday = ?, Tuesday = ?, Wednesday = ?, Thursday = ?, " +
            "Friday = ?, Saturday = ?, Sunday = ? " +
            "WHERE Wk_ID = ?";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                // REQUIRED FIELDS
                ps.setString(1, fromDate);
                ps.setString(2, toDate);

                // OPTIONAL FIELDS (can be null)
                ps.setString(3, monday);
                ps.setString(4, tuesday);
                ps.setString(5, wednesday);
                ps.setString(6, thursday);
                ps.setString(7, friday);
                ps.setString(8, saturday);
                ps.setString(9, sunday);

                ps.setString(10, idParam);

                int rows = ps.executeUpdate();

                if (rows == 0) {
                    sendJson(response, 404, false, "No workout plan found for given id");
                } else {
                    sendJson(response, 200, true, "Workout plan updated successfully");
                }
            }

        } catch (SQLException e) {
            sendJson(response, 500, false, "Database error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            sendJson(response, 500, false, "JDBC driver not found");
        }
    }
}
