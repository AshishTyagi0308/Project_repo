package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

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

    private static final String DB_URL = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Ashish_mca@1234";

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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        if (!isTokenValid(token, response)) {
            return;
        }

        // Get member ID from URL param
        String memberId = request.getParameter("id");

        JSONObject jsonResponse = new JSONObject();
        if (memberId == null) {
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "Member ID (id) is required as a request parameter.");
            response.getWriter().print(jsonResponse.toString());
            return;
        }

        // Parse the JSON body (rest of workout details)
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        try {
            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse(sb.toString());

            // Fetch values from JSON
            String fromDate = (String) body.get("start_date");
            String toDate = (String) body.get("end_date");
            String monday = (String) body.get("monday");
            String tuesday = (String) body.get("tuesday");
            String wednesday = (String) body.get("wednesday");
            String thursday = (String) body.get("thursday");
            String friday = (String) body.get("friday");
            String saturday = (String) body.get("saturday");
            String sunday = (String) body.get("sunday");

            if (fromDate == null || toDate == null) {
                jsonResponse.put("status", "error");
                jsonResponse.put("message", "Required fields (fromDate, toDate) are missing");
                response.getWriter().print(jsonResponse.toString());
                return;
            }

            Connection conn = null;
            PreparedStatement pstmt = null;
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

                String sql = "UPDATE workout_chart SET Start_date = ?, End_date = ?, Monday = ?, Tuesday = ?, Wednesday = ?, " +
                             "Thursday = ?, Friday = ?, Saturday = ?, Sunday = ? WHERE Wk_ID = ?";

                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, fromDate);
                pstmt.setString(2, toDate);
                pstmt.setString(3, monday);
                pstmt.setString(4, tuesday);
                pstmt.setString(5, wednesday);
                pstmt.setString(6, thursday);
                pstmt.setString(7, friday);
                pstmt.setString(8, saturday);
                pstmt.setString(9, sunday);
                pstmt.setString(10, memberId);

                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    jsonResponse.put("status", "success");
                    jsonResponse.put("message", "Workout plan updated successfully");
                } else {
                    jsonResponse.put("status", "error");
                    jsonResponse.put("message", "No workout plan found to update for the given member");
                }
            } catch (Exception e) {
                jsonResponse.put("status", "error");
                jsonResponse.put("message", "Error: " + e.getMessage());
            } finally {
                try {
                    if (pstmt != null) pstmt.close();
                    if (conn != null) conn.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        } catch (Exception e) {
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "JSON Parse Error: " + e.getMessage());
        }
        response.getWriter().print(jsonResponse.toString());
    }
}
