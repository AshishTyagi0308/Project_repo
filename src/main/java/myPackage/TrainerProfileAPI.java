package myPackage;

import org.json.simple.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/TrainerProfileAPI")
public class TrainerProfileAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;

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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
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

        // Read trainer id from parameter: ?id=101
        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSONObject err = new JSONObject();
            err.put("error", "Trainer_ID is required.");
            out.print(err.toJSONString());
            out.flush();
            return;
        }

        int trainerId;
        try {
            trainerId = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSONObject err = new JSONObject();
            err.put("error", "Parameter 'id' must be a valid integer.");
            out.print(err.toJSONString());
            out.flush();
            return;
        }

        String url = "jdbc:mysql://localhost:3306/gym";
        String user = "root";
        String pass = "Ashish_mca@1234";

        String query =
            "SELECT Name, Gender, DOB, Address, Phone_no, Photo, " +
            "       Specialization, Certification, Status " +
            "FROM trainer WHERE Trainer_ID = ?";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject err = new JSONObject();
            err.put("error", "MySQL JDBC Driver not found. " + e.getMessage());
            out.print(err.toJSONString());
            out.flush();
            return;
        }

        try (Connection con = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, trainerId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("name", rs.getString("Name"));
                obj.put("gender", rs.getString("Gender"));
                obj.put("dob", rs.getString("DOB"));
                obj.put("address", rs.getString("Address"));
                obj.put("phone", rs.getString("Phone_no"));

                // Photo stored as TEXT containing full data URL, same pattern as MemberDetail
                String photoString = rs.getString("Photo");
                if (photoString != null && !photoString.isEmpty()) {
                    obj.put("photo", photoString);   // frontend can use directly in <img src={photo}>
                } else {
                    obj.put("photo", null);          // or "" if you prefer
                }

                obj.put("specialization", rs.getString("Specialization"));
                obj.put("certification", rs.getString("Certification"));
                obj.put("status", rs.getString("Status"));

                out.print(obj.toJSONString());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JSONObject err = new JSONObject();
                err.put("error", "Trainer not found for id: " + trainerId);
                out.print(err.toJSONString());
            }

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject err = new JSONObject();
            err.put("error", "SQL Error: " + e.getMessage());
            out.print(err.toJSONString());
        } finally {
            out.flush();
        }
    }

    // PUT allowed with same JWT protection (placeholder for future update logic)
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }

        String token = authHeader.substring(7);
        if (!isTokenValid(token, response)) {
            return;
        }

        PrintWriter out = response.getWriter();
        JSONObject res = new JSONObject();
        res.put("message", "PUT access allowed for TrainerProfileAPI (no update logic implemented yet)");
        out.print(res.toJSONString());
        out.flush();
    }
}
