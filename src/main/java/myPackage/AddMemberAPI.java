package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@SuppressWarnings("unchecked")
@WebServlet("/AddMemberAPI")
public class AddMemberAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Ashish_mca@1234";

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:5173",
            "https://wellness-management-system.vercel.app",
            "https://admonitorial-cinderella-hungerly.ngrok-free.dev"
        );

    protected void addCORSHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "86400");
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        addCORSHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCORSHeaders(request, response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Gson gson = new Gson();
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();

        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        try {
            JsonObject jsonObject = gson.fromJson(sb.toString(), JsonObject.class);

            // Safely get all expected frontend fields using null checks
            String name = jsonObject.has("name") && jsonObject.get("name") != null ? jsonObject.get("name").getAsString().trim() : "";
            String dob = jsonObject.has("dob") && jsonObject.get("dob") != null ? jsonObject.get("dob").getAsString().trim() : "";
            String gender = jsonObject.has("gender") && jsonObject.get("gender") != null ? jsonObject.get("gender").getAsString().trim().toLowerCase() : "";
            String phoneNo = jsonObject.has("phone") && jsonObject.get("phone") != null ? jsonObject.get("phone").getAsString().trim() : "";
            String address = jsonObject.has("address") && jsonObject.get("address") != null ? jsonObject.get("address").getAsString().trim() : "";
            String photo = jsonObject.has("photo") && jsonObject.get("photo") != null ? jsonObject.get("photo").getAsString().trim() : "";

            // Validate input
            if (name.isEmpty() || dob.isEmpty() || gender.isEmpty() || phoneNo.isEmpty() || address.isEmpty()) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "All fields except Photo are required.");
                out.print(gson.toJson(jsonResponse));
                out.flush();
                return;
            }

            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // DO NOT include Member_ID in the statement
                String sql = "INSERT INTO member (Name, DOB, Gender, Phone_no, Address, Photo) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setString(2, dob);
                    ps.setString(3, gender);
                    ps.setString(4, phoneNo);
                    ps.setString(5, address);
                    ps.setString(6, photo);

                    int rowsInserted = ps.executeUpdate();
                    if (rowsInserted > 0) {
                        jsonResponse.addProperty("success", true);
                        jsonResponse.addProperty("message", "Data inserted successfully.");
                    } else {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "Failed to insert data.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Internal server error: " + e.getMessage());
        }

        out.print(gson.toJson(jsonResponse));
        out.flush();
    }
}
