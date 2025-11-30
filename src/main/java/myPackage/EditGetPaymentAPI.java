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

@WebServlet("/EditGetPaymentAPI")
public class EditGetPaymentAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Ashish_mca@1234";

    // JWT secret key (same as token generation)
    private static final byte[] JWT_SECRET =
            "RaJdNoqNevTsnjh9Vgbe/LgPCrbcjwTCfKWpBuOyPTM=".getBytes();

    // JWT validation with error reporting
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

    // Helper method for sending JSON errors
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

        // Authentication: Check Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized: Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        if (!isTokenValid(token, response)) {
            // Error already sent inside isTokenValid
            return;
        }

        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Missing or empty id parameter");
            return;
        }

        String sql =
                "SELECT Payment_ID, Pay_date, Pay_mode, Paid_fee, Due_date " +
                "FROM payment WHERE Payment_ID = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject paymentJson = new JsonObject();
                    paymentJson.addProperty("id", rs.getString("Payment_ID"));
                    paymentJson.addProperty("pay_date", rs.getString("Pay_date"));
                    paymentJson.addProperty("amount", rs.getString("Paid_fee"));
                    paymentJson.addProperty("due_date", rs.getString("Due_date"));
                    paymentJson.addProperty("mode", rs.getString("Pay_mode"));

                    String json = new Gson().toJson(paymentJson);

                    response.setStatus(HttpServletResponse.SC_OK);
                    PrintWriter out = response.getWriter();
                    out.print(json);
                    out.flush();
                } else {
                    // No payment found for this id
                    sendError(response, HttpServletResponse.SC_NOT_FOUND,
                            "Payment not found for id: " + id);
                }
            }
        } catch (SQLException e) {
            // Log on server side
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database error");
        }
    }
}
