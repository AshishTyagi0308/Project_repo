package myPackage;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.Date;

// If using org.json.simple, you must also include json-simple-1.1.1.jar
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@SuppressWarnings("unchecked")
@WebServlet("/LoginPageAPI")
public class LoginPageAPI extends HttpServlet {
    private String dbURL = "jdbc:mysql://localhost:3306/gym";
    private String dbUser = "root";
    private String dbPass = "Ashish_mca@1234";
    private String jwtSecret = "S2rhmJ09N1QfLzq74L0TIjgli8V/0KCE9coNbR7wMbc=";

    private void setCorsHeaders(HttpServletResponse response, HttpServletRequest request, String handler) {
        String allowOrigin = "http://localhost:5173";
        response.setHeader("Access-Control-Allow-Origin", allowOrigin);
        response.setHeader("Vary", "Origin");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "864000");
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response, request, "doOptions");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response, request, "doPost");

        // Add JDBC driver loading
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"JDBC Driver not found\"}");
            return;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String username = null, password = null;
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonRequest = (JSONObject) parser.parse(sb.toString());
            username = (String) jsonRequest.get("username");
            password = (String) jsonRequest.get("password");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid JSON input\"}");
            return;
        }

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Missing username or password\"}");
            return;
        }

        try (Connection conn = DriverManager.getConnection(dbURL, dbUser, dbPass)) {
            String sql = "SELECT * FROM user WHERE Username=? AND Password=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String role = rs.getString("Role");
                    String email = rs.getString("Email");
                    try {
                        String token = Jwts.builder()
                                .setSubject(username)
                                .claim("role", role)
                                .claim("email", email)
                                .setIssuedAt(new Date())
                                .setExpiration(new Date(System.currentTimeMillis() + 12 * 60 * 60 * 1000))
                                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                                .compact();

                        response.setContentType("application/json");
                        response.getWriter().write("{\"token\":\"" + token + "\"}");
                    } catch (Exception jwtEx) {
                        jwtEx.printStackTrace();
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        response.getWriter().write("{\"error\":\"JWT creation failed\"}");
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Invalid username or password\"}");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Server error\"}");
        }
    }
}
