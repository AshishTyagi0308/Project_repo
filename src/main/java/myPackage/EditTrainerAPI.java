package myPackage;

import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import java.sql.*;
import java.sql.Types;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet("/EditTrainerAPI")
public class EditTrainerAPI extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Gson gson = new Gson();
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();

        // ---- TRAINER ID PARAMETER ----
        String trainerId = request.getParameter("id");
        if (trainerId == null || trainerId.trim().isEmpty()) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message",
                    "Missing or empty trainer id (id) parameter.");
            out.print(gson.toJson(jsonResponse));
            return;
        }
        trainerId = trainerId.trim();

        // ---- READ JSON BODY ----
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        JsonObject jsonBody;
        try {
            jsonBody = JsonParser.parseString(sb.toString()).getAsJsonObject();
        } catch (Exception ex) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Invalid JSON body.");
            out.print(gson.toJson(jsonResponse));
            return;
        }

        // ---- SAFE FIELD EXTRACTION ----
        String name           = getSafe(jsonBody, "name");
        String dob            = getSafe(jsonBody, "dob");
        String phone          = getSafe(jsonBody, "phone");
        String address        = getSafe(jsonBody, "address");
        String specialization = getSafe(jsonBody, "specialization");
        String certification  = getSafe(jsonBody, "certification");
        String status         = getSafe(jsonBody, "status");
        String gender         = getSafe(jsonBody, "gender");

        // Photo is optional
        String photo = null;
        if (jsonBody.has("photo")) {
            JsonElement p = jsonBody.get("photo");
            if (p != null && !p.isJsonNull()) {
                String ph = p.getAsString().trim();
                if (!ph.isEmpty()) photo = ph;
            }
        }

        // ---- VALIDATION ----
        if (name.isEmpty() || dob.isEmpty() || phone.isEmpty() ||
            address.isEmpty() || specialization.isEmpty() ||
            certification.isEmpty() || status.isEmpty() || gender.isEmpty()) {

            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "All fields are required.");
            out.print(gson.toJson(jsonResponse));
            return;
        }

        // ---- JDBC UPDATE ----
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {

                String sql =
                    "UPDATE trainer SET Name=?, DOB=?, Phone_no=?, Photo=?, " +
                    "Address=?, Specialization=?, Certification=?, Status=?, Gender=? " +
                    "WHERE Trainer_ID=?";

                try (PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setString(1, name);
                    ps.setString(2, dob);
                    ps.setString(3, phone);

                    if (photo == null)
                        ps.setNull(4, Types.VARCHAR);
                    else
                        ps.setString(4, photo);

                    ps.setString(5, address);
                    ps.setString(6, specialization);
                    ps.setString(7, certification);
                    ps.setString(8, status);
                    ps.setString(9, gender);
                    ps.setString(10, trainerId);

                    int updated = ps.executeUpdate();

                    if (updated > 0) {
                        jsonResponse.addProperty("success", true);
                        jsonResponse.addProperty("message",
                                "Trainer updated successfully.");
                    } else {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message",
                                "Trainer not found.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message",
                    "Internal server error: " + e.getMessage());
        }

        out.print(gson.toJson(jsonResponse));
    }

    // ---- SAFE GETTER ----
    private String getSafe(JsonObject obj, String key) {
        if (!obj.has(key)) return "";
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return "";
        return el.getAsString().trim();
    }
}
