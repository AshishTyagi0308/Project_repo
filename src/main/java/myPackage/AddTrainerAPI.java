package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Types;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet("/AddTrainerAPI")
public class AddTrainerAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Ashish_mca@1234";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // CORS + response config
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        Gson gson = new Gson();
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();

        // Read JSON body
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
            String name = jsonObject.has("name") && jsonObject.get("name") != null
                    ? jsonObject.get("name").getAsString().trim()
                    : "";
            String dob = jsonObject.has("dob") && jsonObject.get("dob") != null
                    ? jsonObject.get("dob").getAsString().trim()
                    : "";
            String phoneNo = jsonObject.has("phone") && jsonObject.get("phone") != null
                    ? jsonObject.get("phone").getAsString().trim()
                    : "";
            // Photo is optional; keep null if not provided or empty
            String photo = jsonObject.has("photo") && jsonObject.get("photo") != null
                    ? jsonObject.get("photo").getAsString().trim()
                    : null;
            if (photo != null && photo.isEmpty()) {
                photo = null;
            }

            String address = jsonObject.has("address") && jsonObject.get("address") != null
                    ? jsonObject.get("address").getAsString().trim()
                    : "";
            String specialization = jsonObject.has("specialization") && jsonObject.get("specialization") != null
                    ? jsonObject.get("specialization").getAsString().trim()
                    : "";
            String certification = jsonObject.has("certification") && jsonObject.get("certification") != null
                    ? jsonObject.get("certification").getAsString().trim()
                    : "";
            String status = jsonObject.has("status") && jsonObject.get("status") != null
                    ? jsonObject.get("status").getAsString().trim()
                    : "";
            String gender = jsonObject.has("gender") && jsonObject.get("gender") != null
                    ? jsonObject.get("gender").getAsString().trim()
                    : "";

            // Validate required input (photo NOT required)
            if (name.isEmpty() || dob.isEmpty() || phoneNo.isEmpty()
                    || address.isEmpty() || specialization.isEmpty()
                    || certification.isEmpty() || status.isEmpty()
                    || gender.isEmpty()) {

                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message",
                        "All fields except Photo are required for trainer.");
                out.print(gson.toJson(jsonResponse));
                out.flush();
                return;
            }

            // JDBC insert (Trainer_ID auto-increment, so not included)
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO trainer "
                        + "(Name, DOB, Phone_no, Photo, Address, Specialization, Certification, Status, Gender) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setString(2, dob);
                    ps.setString(3, phoneNo);

                    // Set photo as NULL if not provided
                    if (photo == null) {
                        ps.setNull(4, Types.VARCHAR);   // Assuming Photo is VARCHAR; use Types.BLOB if BLOB type
                    } else {
                        ps.setString(4, photo);
                    }

                    ps.setString(5, address);
                    ps.setString(6, specialization);
                    ps.setString(7, certification);
                    ps.setString(8, status);
                    ps.setString(9, gender);

                    int rowsInserted = ps.executeUpdate();
                    if (rowsInserted > 0) {
                        jsonResponse.addProperty("success", true);
                        jsonResponse.addProperty("message", "Trainer inserted successfully.");
                    } else {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "Failed to insert trainer.");
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
