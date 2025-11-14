package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
@SuppressWarnings("unchecked")
@WebServlet("/AddNewUserAPI")
public class AddNewUserAPI extends HttpServlet {
	private static final long serialVersionUID = 1L;
    // You can move DB config to environment or config file in production
    private static final String DB_URL = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Ashish_mca@1234";

    private void addCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173"); // Update if deploying
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "864000");
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCORSHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCORSHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Gson gson = new Gson();
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();

        // Read incoming data
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        try {
            // Parse JSON
            JsonObject jsonObject = gson.fromJson(sb.toString(), JsonObject.class);
            String username = jsonObject.get("username").getAsString().trim();
            String password = jsonObject.get("password").getAsString().trim();
            String role = jsonObject.get("role").getAsString().trim().toLowerCase(); // convert to lowercase for DB consistency
            String email = jsonObject.get("email").getAsString().trim();

            // Validate input
            if (username.isEmpty() || password.isEmpty() || role.isEmpty() || email.isEmpty()) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "All fields are required.");
                out.print(gson.toJson(jsonResponse));
                out.flush();
                return;
            }

            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO user (Username, Password, Role, Email) VALUES (?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, role); // ensure value is lowercase
                ps.setString(4, email);

                int rowsInserted = ps.executeUpdate();
                if (rowsInserted > 0) {
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Data inserted successfully.");
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Failed to insert data.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Send detailed error for debugging; restrict this in production for security
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Internal server error: " + e.getMessage());
        }

        out.print(gson.toJson(jsonResponse));
        out.flush();
    }
}