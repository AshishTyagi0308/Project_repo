package myPackage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

// JWT imports
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/Member")
public class Member extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:5173",
        "https://wellness-management-system.vercel.app",
        "https://admonitorial-cinderella-hungerly.ngrok-free.dev"
    );

    // CORS headers
    private void addCORSHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
    }

    // Send JSON error message
    private void sendError(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        JSONObject obj = new JSONObject();
        obj.put("error", msg);
        response.getWriter().print(obj.toJSONString());
    }

    // JWT validation
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
            sendError(response, "Invalid or malformed token");
            return false;
        }
    }

    // Handle preflight
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        addCORSHeaders(req, resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    // POST – Add new member (requires authentication)
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        addCORSHeaders(request, response);

        // 🔒 Check Authorization header
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            sendError(response, "Unauthorized: Missing or invalid Authorization header");
            return;
        }

        String token = auth.substring(7);

        if (!isTokenValid(token, response)) return;

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String name = request.getParameter("name");
        String gender = request.getParameter("gender");
        String dob = request.getParameter("dob");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASS);

            String sql = "INSERT INTO member (name, gender, dob, phone, address) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, name);
            ps.setString(2, gender);
            ps.setString(3, dob);
            ps.setString(4, phone);
            ps.setString(5, address);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    out.println("{\"message\":\"Member added successfully!\", \"unique_id\":" + id + "}");
                }
            } else {
                out.println("{\"message\":\"Failed to add member.\"}");
            }

            con.close();
        } catch (Exception e) {
            out.println("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    // GET – Fetch member by ID (requires authentication)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        addCORSHeaders(request, response);

        // 🔒 Check Authorization
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            sendError(response, "Unauthorized: Missing or invalid Authorization header");
            return;
        }

        String token = auth.substring(7);
        if (!isTokenValid(token, response)) return;

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String idParam = request.getParameter("id");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASS);

            String sql = "SELECT * FROM member WHERE id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, Integer.parseInt(idParam));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getInt("id"));
                obj.put("name", rs.getString("name"));
                obj.put("gender", rs.getString("gender"));
                obj.put("dob", rs.getString("dob"));
                obj.put("phone", rs.getString("phone"));
                obj.put("address", rs.getString("address"));

                out.print(obj.toJSONString());
            } else {
                out.print("{\"message\":\"Member not found\"}");
            }

            con.close();
        } catch (Exception e) {
            out.println("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}