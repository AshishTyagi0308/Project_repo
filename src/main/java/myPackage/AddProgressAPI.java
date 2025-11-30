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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet("/AddProgressAPI")
public class AddProgressAPI extends HttpServlet {

    private static final String URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    // Frontend sends yyyy-MM-dd
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
            // 1. Member id from query parameter
            String memberIdParam = request.getParameter("id");
            if (memberIdParam == null || memberIdParam.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                jsonResp.addProperty("success", false);
                jsonResp.addProperty("message", "Member id is required as parameter 'id'.");
            } else {

                int memberId = Integer.parseInt(memberIdParam);

                // 2. Read JSON body
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = request.getReader()) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                JsonObject body = JsonParser.parseString(sb.toString()).getAsJsonObject();

                // keys from frontend: date, height, weight, arms, chest, shoulder, back,
                // waist, thighs, muscle_percent, fat_percent (bmi ignored)

                String dateStr = body.get("date").getAsString();
                LocalDate localDate = LocalDate.parse(dateStr, INPUT_FMT);
                String dbDateStr = localDate.format(DB_FMT);
                Date sqlDate = Date.valueOf(dbDateStr);

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

                    int rowsInserted = ps.executeUpdate();
                    if (rowsInserted > 0) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        jsonResp.addProperty("success", true);
                        jsonResp.addProperty("message", "Progress data inserted successfully.");
                    } else {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        jsonResp.addProperty("success", false);
                        jsonResp.addProperty("message", "Failed to insert progress data.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResp.addProperty("success", false);
            jsonResp.addProperty("message", "Internal server error: " + e.getMessage());
        }

        // Write JSON response once at the end
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(jsonResp));
            out.flush();
        }
    }
}
