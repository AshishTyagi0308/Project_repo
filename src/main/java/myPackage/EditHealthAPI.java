package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet("/EditHealthAPI")
public class EditHealthAPI extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/gym";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Ashish_mca@1234";

    // Handle PUT from frontend (axios.put)
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);   // reuse same logic for PUT and POST
    }

    // Main logic for update
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        String memberIdParam = request.getParameter("id");
        if (memberIdParam == null || memberIdParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"Missing id parameter\"}");
            return;
        }

        int memberId;
        try {
            memberId = Integer.parseInt(memberIdParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"Invalid id parameter\"}");
            return;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        if (sb.length() == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"Empty JSON body\"}");
            return;
        }

        JsonObject jsonBody = gson.fromJson(sb.toString(), JsonObject.class);

        String medicalHistory = getJsonString(jsonBody, "medical_history");
        String currMedication = getJsonString(jsonBody, "current_medication");
        String allergy = getJsonString(jsonBody, "allergy");
        String surgery = getJsonString(jsonBody, "surgery");
        String injury = getJsonString(jsonBody, "injury");
        String supplement = getJsonString(jsonBody, "supplement");
        String dietPreference = getJsonString(jsonBody, "diet_preference");
        String drink = getJsonString(jsonBody, "drink");
        String smoke = getJsonString(jsonBody, "smoke");

        String sql = "UPDATE health_records SET "
                + "Medical_History = ?, "
                + "Curr_Medication = ?, "
                + "Allergy = ?, "
                + "Surgery = ?, "
                + "Injury = ?, "
                + "Supplement = ?, "
                + "Diet_Preference = ?, "
                + "Drink = ?, "
                + "Smoke = ? "
                + "WHERE Member_ID = ?";

        int rowsAffected = 0;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, medicalHistory);
                ps.setString(2, currMedication);
                ps.setString(3, allergy);
                ps.setString(4, surgery);
                ps.setString(5, injury);
                ps.setString(6, supplement);
                ps.setString(7, dietPreference);
                ps.setString(8, drink);
                ps.setString(9, smoke);
                ps.setInt(10, memberId);

                rowsAffected = ps.executeUpdate();
            }
        } catch (ClassNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\":false,\"message\":\"JDBC driver not found\"}");
            return;
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\":false,\"message\":\"Database error: "
                    + e.getMessage().replace("\"", "'") + "\"}");
            return;
        }

        if (rowsAffected == 0) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.write("{\"success\":false,\"message\":\"No record found for given id\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            out.write("{\"success\":true,\"message\":\"Health record updated successfully\"}");
        }
    }

    private String getJsonString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return obj.get(key).getAsString();
    }
}
