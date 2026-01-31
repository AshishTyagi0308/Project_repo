package myPackage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// JWT imports
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@SuppressWarnings("unchecked")
@WebServlet("/AddHealthRecordAPI")
public class AddHealthRecordAPI extends HttpServlet {

    private static final String URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    private static final String SECRET_KEY = "RaJdNoqNevTsnjh9Vgbe/LgPCrbcjwTCfKWpBuOyPTM=";

    // ---------- helper to read optional strings safely ----------
    private String getStringOrEmpty(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            return "";
        }
        return el.getAsString();
    }

    // ---------------- JWT VALIDATION ----------------
    private boolean isTokenValid(String token, HttpServletResponse response) throws IOException {
        try {
            Jwts.parser()
                .setSigningKey(SECRET_KEY.getBytes())
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            sendError(response, "Invalid token signature");
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            sendError(response, "Token expired");
            return false;
        } catch (Exception e) {
            sendError(response, "Invalid token: " + e.getMessage());
            return false;
        }
    }

    // ---------------- ERROR JSON (auth / validation) ----------------
    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        JSONObject obj = new JSONObject();
        obj.put("success", false);
        obj.put("message", message);

        out.print(obj.toJSONString());
        out.flush();
    }

    // ---------------- MAIN POST API ----------------
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        JSONObject jsonResponse = new JSONObject();

        // 1) Authorization Header
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(resp, "Unauthorized: Missing/Invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp)) {
            return;
        }

        // 2) Read ID from request parameter
        String member_id = req.getParameter("id");
        if (member_id == null || member_id.isEmpty()) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "member_id (id) is required");
            out.print(jsonResponse.toJSONString());
            out.flush();
            return;
        }

        // 3) Read JSON Body
        BufferedReader reader = req.getReader();
        JsonElement root = JsonParser.parseReader(reader);

        if (root == null || !root.isJsonObject()) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Request body must be a valid JSON object");
            out.print(jsonResponse.toJSONString());
            out.flush();
            return;
        }

        JsonObject jsonBody = root.getAsJsonObject();

        // Safely read all fields (empty string if missing / null)
        String medical_history  = getStringOrEmpty(jsonBody, "medical_history");
        String curr_medication  = getStringOrEmpty(jsonBody, "current_medication");
        String allergy          = getStringOrEmpty(jsonBody, "allergy");
        String surgery          = getStringOrEmpty(jsonBody, "surgery");
        String injury           = getStringOrEmpty(jsonBody, "injury");
        String supplement       = getStringOrEmpty(jsonBody, "supplement");
        String diet_preference  = getStringOrEmpty(jsonBody, "diet_preference");
        String drink            = getStringOrEmpty(jsonBody, "drink");
        String smoke            = getStringOrEmpty(jsonBody, "smoke");

        Connection con = null;
        PreparedStatement ps = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            // **NEW: Check if record already exists for this member_id**
            String checkSql = "SELECT COUNT(*) FROM health_records WHERE Member_ID = ?";
            PreparedStatement checkPs = con.prepareStatement(checkSql);
            checkPs.setString(1, member_id);
            ResultSet rs = checkPs.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Health record already exists for this member. Delete existing record first.");
                out.print(jsonResponse.toJSONString());
                out.flush();
                
                rs.close();
                checkPs.close();
                return;
            }
            rs.close();
            checkPs.close();

            // **Proceed with INSERT only if no existing record**
            String sql = "INSERT INTO health_records "
                    + "(Member_ID, Medical_History, Curr_Medication, Allergy, Surgery, Injury, "
                    + "Supplement, Diet_Preference, Drink, Smoke) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            ps = con.prepareStatement(sql);
            ps.setString(1,  member_id);
            ps.setString(2,  medical_history);
            ps.setString(3,  curr_medication);
            ps.setString(4,  allergy);
            ps.setString(5,  surgery);
            ps.setString(6,  injury);
            ps.setString(7,  supplement);
            ps.setString(8,  diet_preference);
            ps.setString(9,  drink);
            ps.setString(10, smoke);

            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                jsonResponse.put("success", true);
                jsonResponse.put("message", "Health Record Added Successfully");
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Failed to add health record - no rows affected");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Failed to add health record: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            try { if (con != null) con.close(); } catch (Exception ignored) {}
        }

        out.print(jsonResponse.toJSONString());
        out.flush();
    }
}
