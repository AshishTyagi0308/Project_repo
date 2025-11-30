package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/EditGetWorkoutAPI")
public class EditGetWorkoutAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Ashish_mca@1234";

    // JWT secret key (same as token generation)
    private static final byte[] JWT_SECRET =
            "RaJdNoqNevTsnjh9Vgbe/LgPCrbcjwTCfKWpBuOyPTM=".getBytes();

    // JWT validation with error reporting
    private boolean isTokenValid(String token, HttpServletResponse response) throws IOException {
        try {
            Jwts.parser()
                .setSigningKey(JWT_SECRET)
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token signature");
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
            return false;
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Malformed or invalid token");
            return false;
        }
    }

    // Helper method for sending JSON errors
    private void sendError(HttpServletResponse response, int statusCode, String message)
            throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject err = new JSONObject();
        err.put("error", message);

        PrintWriter out = response.getWriter();
        out.print(err.toJSONString());
        out.flush();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Authentication: Check Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        if (!isTokenValid(token, response)) {
            // Error already sent inside isTokenValid
            return;
        }

        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Missing or empty id parameter");
            return;
        }

        // Query to fetch from workout_chart table, assuming id maps to Workout_ID
        String sql =
                "SELECT Wk_ID, Start_date, End_date, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday " +
                "FROM workout_chart WHERE Wk_ID = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject workoutJson = new JsonObject();
                    workoutJson.addProperty("id", rs.getString("Wk_ID"));
                    workoutJson.addProperty("start_date", rs.getString("Start_date"));
                    workoutJson.addProperty("end_date", rs.getString("End_date"));
                    workoutJson.addProperty("monday", rs.getString("Monday"));
                    workoutJson.addProperty("tuesday", rs.getString("Tuesday"));
                    workoutJson.addProperty("wednesday", rs.getString("Wednesday"));
                    workoutJson.addProperty("thursday", rs.getString("Thursday"));
                    workoutJson.addProperty("friday", rs.getString("Friday"));
                    workoutJson.addProperty("saturday", rs.getString("Saturday"));
                    workoutJson.addProperty("sunday", rs.getString("Sunday"));

                    String json = new Gson().toJson(workoutJson);

                    response.setStatus(HttpServletResponse.SC_OK);
                    PrintWriter out = response.getWriter();
                    out.print(json);
                    out.flush();
                } else {
                    sendError(response, HttpServletResponse.SC_NOT_FOUND,
                            "Workout not found for id: " + id);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database error");
        }
    }
}
