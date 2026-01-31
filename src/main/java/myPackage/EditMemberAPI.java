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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@SuppressWarnings("unchecked")
@WebServlet("/EditMemberAPI")
public class EditMemberAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Ashish_mca@1234";

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

    // Helper method for sending JSON errors (401)
    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        JSONObject err = new JSONObject();
        err.put("success", false);
        err.put("message", message);
        out.print(err.toJSONString());
        out.flush();
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        JsonObject jsonResponse = new JsonObject();

        // 1. Auth header check
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }

        String token = authHeader.substring(7);
        if (!isTokenValid(token, response)) {
            return; // error already sent
        }

        // 2. Member ID from query param ?id=...
        String oldMemberId = request.getParameter("id");
        if (oldMemberId == null || oldMemberId.trim().isEmpty()) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Missing or empty member id (id) parameter.");
            out.print(gson.toJson(jsonResponse));
            out.flush();
            return;
        }
        oldMemberId = oldMemberId.trim();

        // 3. Read JSON body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JsonObject jsonData;
        try {
            jsonData = gson.fromJson(sb.toString(), JsonObject.class);
        } catch (Exception e) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Invalid JSON body.");
            out.print(gson.toJson(jsonResponse));
            out.flush();
            return;
        }

        if (jsonData == null) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Empty JSON body.");
            out.print(gson.toJson(jsonResponse));
            out.flush();
            return;
        }

        // 4. Safely read fields (avoid JsonNull.getAsString())
        String name   = jsonData.has("name")   && jsonData.get("name")   != null && !jsonData.get("name").isJsonNull()
                        ? jsonData.get("name").getAsString().trim() : "";
        String dob    = jsonData.has("dob")    && jsonData.get("dob")    != null && !jsonData.get("dob").isJsonNull()
                        ? jsonData.get("dob").getAsString().trim() : "";
        String gender = jsonData.has("gender") && jsonData.get("gender") != null && !jsonData.get("gender").isJsonNull()
                        ? jsonData.get("gender").getAsString().trim().toLowerCase() : "";
        String phone  = jsonData.has("phone")  && jsonData.get("phone")  != null && !jsonData.get("phone").isJsonNull()
                        ? jsonData.get("phone").getAsString().trim() : "";
        String address= jsonData.has("address")&& jsonData.get("address")!= null && !jsonData.get("address").isJsonNull()
                        ? jsonData.get("address").getAsString().trim() : "";
        String photo  = jsonData.has("photo")  && jsonData.get("photo")  != null && !jsonData.get("photo").isJsonNull()
                        ? jsonData.get("photo").getAsString().trim() : "";

        // 5. Validation: same rule as AddMemberAPI (photo optional)
        if (name.isEmpty() || dob.isEmpty() || gender.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "All fields except Photo are required.");
            out.print(gson.toJson(jsonResponse));
            out.flush();
            return;
        }

        // 6. DB update
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            String sql = "UPDATE member SET Name=?, DOB=?, Gender=?, Phone_no=?, Address=?, Photo=? WHERE Member_ID=?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, dob);
            stmt.setString(3, gender);
            stmt.setString(4, phone);
            stmt.setString(5, address);
            stmt.setString(6, photo);
            stmt.setString(7, oldMemberId);

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Member updated successfully.");
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Member not found.");
            }

        } catch (ClassNotFoundException e) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "JDBC Driver not found.");
        } catch (SQLException e) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Database error: " + e.getMessage());
        } finally {
            try {
                    if (stmt != null) stmt.close();
                    if (conn != null) conn.close();
                } catch (SQLException ignored) {}
           }

           // 7. Send final JSON response
           out.print(gson.toJson(jsonResponse));
           out.flush();
           }
}
