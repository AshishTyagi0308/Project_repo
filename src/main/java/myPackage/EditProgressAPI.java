package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/EditProgressAPI")
public class EditProgressAPI extends HttpServlet {

    private static final String URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DB_FMT    = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ===== JWT validation =====
    private boolean isTokenValid(String token, HttpServletResponse response) throws IOException {
        try {
            Jwts.parser()
                .setSigningKey("RaJdNoqNevTsnjh9Vgbe/LgPCrbcjwTCfKWpBuOyPTM=".getBytes())
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token signature");
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
            return false;
        } catch (Exception e) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Malformed or invalid token: " + e.getMessage());
            return false;
        }
    }

    // unified error JSON
    private void sendJsonError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JSONObject err = new JSONObject();
        err.put("success", false);
        err.put("message", message);
        PrintWriter out = response.getWriter();
        out.print(err.toJSONString());
        out.flush();
    }

    private void sendJsonOk(HttpServletResponse response, JSONObject obj) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print(obj.toJSONString());
        out.flush();
    }

    // ===== Helper: parse float safely =====
    private Float parseRequiredFloat(Object value) {
        if (value == null) return null;
        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    // ========== GET: fetch existing progress row ==========
    private void handleGetExistingProgress(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // JWT check
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp)) return;

        String progressIdParam = req.getParameter("id");
        if (progressIdParam == null || progressIdParam.trim().isEmpty()) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Progress_ID (id param) is required");
            return;
        }

        int progressId;
        try {
            progressId = Integer.parseInt(progressIdParam);
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Progress_ID must be a valid integer");
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            String sql = "SELECT Progress_ID, Member_ID, Date, Height, Weight, Arms, Chest, "
                    + "Shoulder, Back, Waist, Thighs, Muscle, Fat "
                    + "FROM progress_chart WHERE Progress_ID = ?";
            ps = con.prepareStatement(sql);
            ps.setInt(1, progressId);
            rs = ps.executeQuery();

            if (rs.next()) {
                JSONObject obj = new JSONObject();

                obj.put("progress_id", rs.getInt("Progress_ID"));
                obj.put("member_id", rs.getInt("Member_ID"));

                Date date = rs.getDate("Date");
                obj.put("date", date.toLocalDate().format(DB_FMT));

                obj.put("height", rs.getFloat("Height"));
                obj.put("weight", rs.getFloat("Weight"));
                obj.put("arms", rs.getFloat("Arms"));
                obj.put("chest", rs.getFloat("Chest"));
                obj.put("shoulder", rs.getFloat("Shoulder"));
                obj.put("back", rs.getFloat("Back"));
                obj.put("waist", rs.getFloat("Waist"));
                obj.put("thighs", rs.getFloat("Thighs"));
                obj.put("muscle_percent", rs.getFloat("Muscle"));
                obj.put("fat_percent", rs.getFloat("Fat"));

                obj.put("success", true);
                obj.put("message", "Progress record fetched successfully");

                sendJsonOk(resp, obj);

            } else {
                sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND,
                        "No progress record found with this Progress_ID");
            }

        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database/server error: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }

    // ========== PUT: update row ==========
    private void handleUpdateProgress(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // JWT check
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp)) return;

        String progressIdParam = req.getParameter("id");
        if (progressIdParam == null || progressIdParam.trim().isEmpty()) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Progress_ID (id param) is required");
            return;
        }

        int progressId;
        try {
            progressId = Integer.parseInt(progressIdParam);
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Progress_ID must be a valid integer");
            return;
        }

        // Read JSON body
        String body;
        try (BufferedReader reader = req.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            body = sb.toString();
        }

        if (body.trim().isEmpty()) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "All fields are required.");
            return;
        }

        JSONObject json;
        try {
            json = (JSONObject) new JSONParser().parse(body);
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "All fields are required.");
            return;
        }

        // Read values
        String dateStr = (json.get("date") == null) ? null : json.get("date").toString().trim();

        Float heightCm = parseRequiredFloat(json.get("height"));
        Float weightKg = parseRequiredFloat(json.get("weight"));
        Float arms     = parseRequiredFloat(json.get("arms"));
        Float chest    = parseRequiredFloat(json.get("chest"));
        Float shoulder = parseRequiredFloat(json.get("shoulder"));
        Float back     = parseRequiredFloat(json.get("back"));
        Float waist    = parseRequiredFloat(json.get("waist"));
        Float thighs   = parseRequiredFloat(json.get("thighs"));
        Float muscle   = parseRequiredFloat(json.get("muscle_percent"));
        Float fat      = parseRequiredFloat(json.get("fat_percent"));

        // FINAL unified validation
        if (dateStr == null || dateStr.isEmpty() ||
            heightCm == null || weightKg == null || arms == null ||
            chest == null || shoulder == null || back == null ||
            waist == null || thighs == null || muscle == null || fat == null) {

            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "All fields are required.");
            return;
        }

        // Convert date
        Date sqlDate;
        try {
            LocalDate ld = LocalDate.parse(dateStr, INPUT_FMT);
            sqlDate = Date.valueOf(ld.format(DB_FMT));
        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "All fields are required.");
            return;
        }

        // Update DB
        Connection con = null;
        PreparedStatement ps = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            String query = "UPDATE progress_chart SET "
                    + "Date=?, Height=?, Weight=?, Arms=?, Chest=?, "
                    + "Shoulder=?, Back=?, Waist=?, Thighs=?, Muscle=?, Fat=? "
                    + "WHERE Progress_ID=?";

            ps = con.prepareStatement(query);
            ps.setDate(1, sqlDate);
            ps.setFloat(2, heightCm);
            ps.setFloat(3, weightKg);
            ps.setFloat(4, arms);
            ps.setFloat(5, chest);
            ps.setFloat(6, shoulder);
            ps.setFloat(7, back);
            ps.setFloat(8, waist);
            ps.setFloat(9, thighs);
            ps.setFloat(10, muscle);
            ps.setFloat(11, fat);
            ps.setInt(12, progressId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                JSONObject obj = new JSONObject();
                obj.put("success", true);
                obj.put("message", "Progress updated successfully");
                obj.put("progress_id", progressId);

                sendJsonOk(resp, obj);
            } else {
                sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND,
                        "No progress record found with this Progress_ID");
            }

        } catch (Exception e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database/server error: " + e.getMessage());
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }

    // ====== doGet ======
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        handleGetExistingProgress(req, resp);
    }

    // ====== doPut ======
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        handleUpdateProgress(req, resp);
    }
}
