package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@SuppressWarnings("unchecked")
@WebServlet("/AddMemberAPI")
public class AddMemberAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Ashish_mca@1234";

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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        if (!isTokenValid(token, response)) {
            // Error sent inside isTokenValid; return directly
            return;
        }
        Gson gson = new Gson();
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();

        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        try {
            JsonObject jsonObject = gson.fromJson(sb.toString(), JsonObject.class);

            // Safely get all expected frontend fields using null checks
            String name = jsonObject.has("name") && jsonObject.get("name") != null ? jsonObject.get("name").getAsString().trim() : "";
            String dob = jsonObject.has("dob") && jsonObject.get("dob") != null ? jsonObject.get("dob").getAsString().trim() : "";
            String gender = jsonObject.has("gender") && jsonObject.get("gender") != null ? jsonObject.get("gender").getAsString().trim().toLowerCase() : "";
            String phoneNo = jsonObject.has("phone") && jsonObject.get("phone") != null ? jsonObject.get("phone").getAsString().trim() : "";
            String address = jsonObject.has("address") && jsonObject.get("address") != null ? jsonObject.get("address").getAsString().trim() : "";
            String photo = jsonObject.has("photo") && jsonObject.get("photo") != null ? jsonObject.get("photo").getAsString().trim() : "";

            // Validate input
            if (name.isEmpty() || dob.isEmpty() || gender.isEmpty() || phoneNo.isEmpty() || address.isEmpty()) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "All fields except Photo are required.");
                out.print(gson.toJson(jsonResponse));
                out.flush();
                return;
            }

            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // DO NOT include Member_ID in the statement
                String sql = "INSERT INTO member (Name, DOB, Gender, Phone_no, Address, Photo) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setString(2, dob);
                    ps.setString(3, gender);
                    ps.setString(4, phoneNo);
                    ps.setString(5, address);
                    ps.setString(6, photo);

                    int rowsInserted = ps.executeUpdate();
                    if (rowsInserted > 0) {
                        jsonResponse.addProperty("success", true);
                        jsonResponse.addProperty("message", "Data inserted successfully.");
                    } else {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "Failed to insert data.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Internal server error: " + e.getMessage());
        }

        out.print(gson.toJson(jsonResponse));
        out.flush();
    }
}
