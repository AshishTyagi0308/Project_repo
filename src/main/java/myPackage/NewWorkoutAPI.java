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
        JSONObject err = new JSONObject();
        err.put("success", false);
        err.put("message", message);
        response.getWriter().print(err.toJSONString());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject json = new JSONObject();

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }

        String token = authHeader.substring(7);
        if (!isTokenValid(token, response)) {
            return;
        }

        try {
            String memberIdStr = request.getParameter("id");
            if (memberIdStr == null) {
                json.put("success", false);
                json.put("message", "MemberID missing");
                out.print(json);
                return;
            }
            int memberID = Integer.parseInt(memberIdStr);

            // Read JSON body
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            JsonObject body = JsonParser.parseString(sb.toString()).getAsJsonObject();

            String start_date = body.has("start_date") ? body.get("start_date").getAsString() : null;
            String end_date   = body.has("end_date")   ? body.get("end_date").getAsString() : null;

            // ❗ REQUIRED FIELD CHECK
            if (start_date == null || start_date.trim().isEmpty() ||
                end_date == null || end_date.trim().isEmpty()) {

                json.put("success", false);
                json.put("message", "Dates are required");
                out.print(json);
                return;
            }

            // Optional fields (can be null)
            String monday    = body.has("monday") ? body.get("monday").getAsString() : null;
            String tuesday   = body.has("tuesday") ? body.get("tuesday").getAsString() : null;
            String wednesday = body.has("wednesday") ? body.get("wednesday").getAsString() : null;
            String thursday  = body.has("thursday") ? body.get("thursday").getAsString() : null;
            String friday    = body.has("friday") ? body.get("friday").getAsString() : null;
            String saturday  = body.has("saturday") ? body.get("saturday").getAsString() : null;
            String sunday    = body.has("sunday") ? body.get("sunday").getAsString() : null;

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
            out.print(json);
        }
    }
}
