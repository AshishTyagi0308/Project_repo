package myPackage;

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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet("/NewWorkoutAPI")
public class NewWorkoutAPI extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // REMOVED: ALLOWED_ORIGINS and setCORSHeaders (CORS now handled by CORSFilter)

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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        // Authentication: Check Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7); // Remove "Bearer " prefix

        if (!isTokenValid(token, response)) {
            return;
        }

        JSONObject json = new JSONObject();

        try {
            String memberIdStr = request.getParameter("id");
            if (memberIdStr == null) {
                json.put("success", false);
                json.put("message", "MemberID missing");
                out.print(json);
                return;
            }
            int memberID = Integer.parseInt(memberIdStr);

            // Read JSON body params
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            JsonObject body = JsonParser.parseString(sb.toString()).getAsJsonObject();

            String start_date = body.get("start_date").getAsString();
            String end_date = body.get("end_date").getAsString();
            String monday = body.get("monday").getAsString();
            String tuesday = body.get("tuesday").getAsString();
            String wednesday = body.get("wednesday").getAsString();
            String thursday = body.get("thursday").getAsString();
            String friday = body.get("friday").getAsString();
            String saturday = body.get("saturday").getAsString();
            String sunday = body.get("sunday").getAsString();

            // Now insert into DB
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/gym", "root", "Ashish_mca@1234");

            String sql = "INSERT INTO workout_chart(Member_ID, Start_date, End_date, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, memberID);
            ps.setString(2, start_date);
            ps.setString(3, end_date);
            ps.setString(4, monday);
            ps.setString(5, tuesday);
            ps.setString(6, wednesday);
            ps.setString(7, thursday);
            ps.setString(8, friday);
            ps.setString(9, saturday);
            ps.setString(10, sunday);

            int status = ps.executeUpdate();
            json.put("success", status > 0);
            json.put("message", status > 0 ? "Workout Plan Added Successfully" : "Failed to Add Workout Plan");
            out.print(json);
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            json.put("success", false);
            json.put("error", e.getMessage());
            json.put("exception", e.toString());
            StringBuilder stackTraceBuilder = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                stackTraceBuilder.append(element.toString()).append("\n");
            }
            json.put("stackTraceString", stackTraceBuilder.toString());
            out.print(json);
        }
    }

    // REMOVED doOptions (CORS handled globally by filter)
}