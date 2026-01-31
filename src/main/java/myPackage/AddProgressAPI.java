package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet("/AddProgressAPI")
public class AddProgressAPI extends HttpServlet {

    private static final String URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DB_FMT    = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Gson gson = new Gson();
        JsonObject jsonResp = new JsonObject();

        try {
            // ----- MEMBER ID -----
            String memberIdParam = request.getParameter("id");
            if (memberIdParam == null || memberIdParam.trim().isEmpty()) {
                sendError(response, gson, "Member id is required as parameter 'id'.");
                return;
            }

            int memberId = Integer.parseInt(memberIdParam);

            // ----- READ BODY -----
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }

            JsonObject body = JsonParser.parseString(sb.toString()).getAsJsonObject();

            // ----- VALIDATION FIXED -----
            String[] requiredFields = {
                "date","height","weight","arms","chest",
                "shoulder","back","waist","thighs",
                "muscle_percent","fat_percent"
            };

            for (String field : requiredFields) {
                if (!body.has(field)) {
                    sendError(response, gson, "All fields are required.");
                    return;
                }

                JsonElement el = body.get(field);

                if (el == null || el.isJsonNull()) {
                    sendError(response, gson, "All fields are required.");
                    return;
                }

                // If string → check empty
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                    if (el.getAsString().trim().isEmpty()) {
                        sendError(response, gson, "All fields are required.");
                        return;
                    }
                }
            }

            // ----- DATE -----
            LocalDate localDate = LocalDate.parse(body.get("date").getAsString(), INPUT_FMT);
            Date sqlDate = Date.valueOf(localDate.format(DB_FMT));

            // ----- FLOAT VALUES -----
            float heightCm = body.get("height").getAsFloat();
            float weightKg = body.get("weight").getAsFloat();
            float arms     = body.get("arms").getAsFloat();
            float chest    = body.get("chest").getAsFloat();
            float shoulder = body.get("shoulder").getAsFloat();
            float back     = body.get("back").getAsFloat();
            float waist    = body.get("waist").getAsFloat();
            float thighs   = body.get("thighs").getAsFloat();
            float muscle   = body.get("muscle_percent").getAsFloat();
            float fat      = body.get("fat_percent").getAsFloat();

            Class.forName("com.mysql.cj.jdbc.Driver");

            // ----- SQL INSERT -----
            try (Connection con = DriverManager.getConnection(URL, USER, PASS);
                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO progress_chart "
                    + "(Member_ID, Date, Height, Weight, Arms, Chest, Shoulder, Back, Waist, Thighs, Muscle, Fat) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")) {

                ps.setInt(1, memberId);
                ps.setDate(2, sqlDate);
                ps.setFloat(3, heightCm);
                ps.setFloat(4, weightKg);
                ps.setFloat(5, arms);
                ps.setFloat(6, chest);
                ps.setFloat(7, shoulder);
                ps.setFloat(8, back);
                ps.setFloat(9, waist);
                ps.setFloat(10, thighs);
                ps.setFloat(11, muscle);
                ps.setFloat(12, fat);

                int rows = ps.executeUpdate();

                if (rows > 0) {
                    jsonResp.addProperty("success", true);
                    jsonResp.addProperty("message", "Progress data inserted successfully.");
                } else {
                    sendError(response, gson, "Failed to insert progress data.");
                    return;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, gson, "Internal server error: " + e.getMessage());
            return;
        }

        // ----- SEND SUCCESS -----
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(jsonResp));
            out.flush();
        }
    }


    // ----- REUSABLE ERROR METHOD -----
    private void sendError(HttpServletResponse response, Gson gson, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        JsonObject err = new JsonObject();
        err.addProperty("success", false);
        err.addProperty("message", msg);
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(err));
        }
    }
}
