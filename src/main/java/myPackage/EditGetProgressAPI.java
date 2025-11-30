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

@WebServlet("/EditGetProgressAPI")
public class EditGetProgressAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String DB_URL      = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER     = "root";
    private static final String DB_PASSWORD = "Ashish_mca@1234";

    // Same secret as other APIs
    private static final byte[] JWT_SECRET =
            "RaJdNoqNevTsnjh9Vgbe/LgPCrbcjwTCfKWpBuOyPTM=".getBytes();

    // JWT validation
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

    // JSON error helper
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

        // 1. JWT check
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                      "Unauthorized: Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        if (!isTokenValid(token, response)) {
            return;
        }

        // 2. Progress_ID from query param ?id=
        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                      "Missing or empty id parameter");
            return;
        }

        String sql =
            "SELECT Progress_ID, Member_ID, Date, Height, Weight, Arms, Chest, " +
            "Shoulder, Back, Waist, Thighs, Muscle, Fat " +
            "FROM progress_chart WHERE Progress_ID = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject progressJson = new JsonObject();

                    progressJson.addProperty("progress_id", rs.getInt("Progress_ID"));
                    progressJson.addProperty("member_id", rs.getInt("Member_ID"));
                    progressJson.addProperty("date", rs.getString("Date"));
                    progressJson.addProperty("height", rs.getFloat("Height"));
                    progressJson.addProperty("weight", rs.getFloat("Weight"));
                    progressJson.addProperty("arms", rs.getFloat("Arms"));
                    progressJson.addProperty("chest", rs.getFloat("Chest"));
                    progressJson.addProperty("shoulder", rs.getFloat("Shoulder"));
                    progressJson.addProperty("back", rs.getFloat("Back"));
                    progressJson.addProperty("waist", rs.getFloat("Waist"));
                    progressJson.addProperty("thighs", rs.getFloat("Thighs"));
                    progressJson.addProperty("muscle_percent", rs.getFloat("Muscle"));
                    progressJson.addProperty("fat_percent", rs.getFloat("Fat"));

                    String json = new Gson().toJson(progressJson);

                    response.setStatus(HttpServletResponse.SC_OK);
                    PrintWriter out = response.getWriter();
                    out.print(json);
                    out.flush();
                } else {
                    sendError(response, HttpServletResponse.SC_NOT_FOUND,
                              "Progress not found for id: " + id);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                      "Database error");
        }
    }
}
