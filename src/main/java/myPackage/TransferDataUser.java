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

// JWT imports
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@SuppressWarnings("unchecked")
@WebServlet("/TransferDataUser")
public class TransferDataUser extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // REMOVED: ALLOWED_ORIGINS and addCORSHeaders logic.
    // All CORS logic now moved to CORSFilter.

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

    // Handle GET request with authentication check
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // REMOVED: addCORSHeaders(request, response);  (Filter handles CORS)

        // Authentication: Check Authorization header
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

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        JSONArray usersArray = new JSONArray();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/gym", "root", "Ashish_mca@1234");

            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM user");

            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                JSONObject user = new JSONObject();

                user.put("username", rs.getString("Username"));
                user.put("password", rs.getString("Password"));
                user.put("role", rs.getString("Role"));
                user.put("email", rs.getString("Email"));

                usersArray.add(user);
            }

            if (!hasData) {
                System.out.println("No data found in user table.");
            }

            rs.close();
            st.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject err = new JSONObject();
            err.put("error", e.getMessage());
            out.print(err.toJSONString());
            out.flush();
            return;
        }

        out.print(usersArray.toJSONString());
        out.flush();
    }

    // REMOVED: doOptions override for CORS (now handled by filter)
}