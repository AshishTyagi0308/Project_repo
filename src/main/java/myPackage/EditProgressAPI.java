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

    // ====== Public HTTP methods (required to avoid 405) ======
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleGetExistingProgress(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleUpdateProgress(req, resp);
    }

    // ====== JWT validation ======
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
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        JSONObject err = new JSONObject();
        err.put("error", message);
        PrintWriter out = response.getWriter();
        out.print(err.toJSONString());
        out.flush();
    }

    // ========== GET: fetch existing row for prefill ==========
    private void handleGetExistingProgress(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // JWT
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(resp, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp)) return;

        String progressIdParam = req.getParameter("id");
        if (progressIdParam == null || progressIdParam.trim().isEmpty()) {
            sendError(resp, "Progress_ID (id param) is required");
            return;
        }

        int progressId;
        try {
            progressId = Integer.parseInt(progressIdParam);
        } catch (NumberFormatException e) {
            sendError(resp, "Progress_ID must be a valid integer");
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            String sql = "SELECT Progress_ID, Member_ID, Date, Height, Weight, Arms, Chest, " +
                         "Shoulder, Back, Waist, Thighs, Muscle, Fat " +
                         "FROM progress_chart WHERE Progress_ID = ?";
            ps = con.prepareStatement(sql);
            ps.setInt(1, progressId);
            rs = ps.executeQuery();

            if (rs.next()) {
                JSONObject obj = new JSONObject();

                obj.put("progress_id", rs.getInt("Progress_ID"));
                obj.put("member_id", rs.getInt("Member_ID"));

                Date date = rs.getDate("Date");
                String dateStr = date.toLocalDate().format(DB_FMT);
                obj.put("date", dateStr);

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

                resp.setStatus(HttpServletResponse.SC_OK);
                PrintWriter out = resp.getWriter();
                out.print(obj.toJSONString());
                out.flush();
            } else {
                sendError(resp, "No progress record found with this Progress_ID");
            }

        } catch (Exception e) {
            sendError(resp, "Database/server error: " + e.getMessage());
        } finally {
            try { if (rs  != null) rs.close(); } catch (Exception e) {}
            try { if (ps  != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }

    // ========== PUT: update row with JSON body ==========
    private void handleUpdateProgress(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        JSONObject obj = new JSONObject();

        // JWT
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(resp, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }
        String token = authHeader.substring(7);
        if (!isTokenValid(token, resp)) return;

        String progressIdParam = req.getParameter("id");
        if (progressIdParam == null || progressIdParam.trim().isEmpty()) {
            sendError(resp, "Progress_ID (id param) is required");
            return;
        }

        int progressId;
        try {
            progressId = Integer.parseInt(progressIdParam);
        } catch (NumberFormatException e) {
            sendError(resp, "Progress_ID must be a valid integer");
            return;
        }

        // Read JSON body (same fields as AddProgressAPI)
        String dateStr;
        float heightCm, weightKg;
        float arms, chest, shoulder, back;
        float waist, thighs, muscle, fat;

        try {
            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String body = sb.toString();
            if (body.isEmpty()) {
                sendError(resp, "Request body is empty. Data required in JSON format.");
                return;
            }

            JSONObject json = (JSONObject) new JSONParser().parse(body);

            dateStr   = (String) json.get("date");
            heightCm  = Float.parseFloat(json.get("height").toString());
            weightKg  = Float.parseFloat(json.get("weight").toString());
            arms      = Float.parseFloat(json.get("arms").toString());
            chest     = Float.parseFloat(json.get("chest").toString());
            shoulder  = Float.parseFloat(json.get("shoulder").toString());
            back      = Float.parseFloat(json.get("back").toString());
            waist     = Float.parseFloat(json.get("waist").toString());
            thighs    = Float.parseFloat(json.get("thighs").toString());
            muscle    = Float.parseFloat(json.get("muscle_percent").toString());
            fat       = Float.parseFloat(json.get("fat_percent").toString());

        } catch (Exception e) {
            sendError(resp, "Failed to parse JSON: " + e.getMessage());
            return;
        }

        // Convert date
        Date sqlDate;
        try {
            LocalDate localDate = LocalDate.parse(dateStr, INPUT_FMT);
            String dbDateStr = localDate.format(DB_FMT);
            sqlDate = Date.valueOf(dbDateStr);
        } catch (Exception e) {
            sendError(resp, "Invalid date format. Expected yyyy-MM-dd");
            return;
        }

        // JDBC update
        Connection con = null;
        PreparedStatement ps = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            String query =
                "UPDATE progress_chart SET " +
                "Date = ?, Height = ?, Weight = ?, Arms = ?, Chest = ?, " +
                "Shoulder = ?, Back = ?, Waist = ?, Thighs = ?, Muscle = ?, Fat = ? " +
                "WHERE Progress_ID = ?";

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
                resp.setStatus(HttpServletResponse.SC_OK);
                obj.put("success", true);
                obj.put("message", "Progress updated successfully");
                obj.put("progress_id", progressId);
                obj.put("date", dateStr);
                obj.put("height", heightCm);
                obj.put("weight", weightKg);
                obj.put("arms", arms);
                obj.put("chest", chest);
                obj.put("shoulder", shoulder);
                obj.put("back", back);
                obj.put("waist", waist);
                obj.put("thighs", thighs);
                obj.put("muscle_percent", muscle);
                obj.put("fat_percent", fat);

                out.print(obj.toJSONString());
                out.flush();
            } else {
                sendError(resp, "No progress record found with this Progress_ID");
            }

        } catch (Exception e) {
            sendError(resp, "Database/server error: " + e.getMessage());
        } finally {
            try { if (ps  != null) ps.close();  } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }
}
