package myPackage;

import org.json.simple.JSONObject;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.*;

@WebServlet("/HealthRecordFetchAPI")
public class HealthRecordFetchAPI extends HttpServlet {

    private static final String URL  = "jdbc:mysql://localhost:3306/gym";      // <- your DB
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    // Support GET (e.g., axios.get) and POST
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleFetch(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleFetch(request, response);
    }

    private void handleFetch(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        JSONObject obj = new JSONObject();

        try {
            // Read member ID from request
            String memberID = request.getParameter("id");

            if (memberID == null || memberID.trim().isEmpty()) {
                obj.put("status", "error");
                obj.put("message", "member_id is required");
                out.print(obj.toJSONString());
                return;
            }

            // DB connection
            Class.forName("com.mysql.cj.jdbc.Driver");  // modern driver
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM health_records WHERE Member_ID = ?")) {

                ps.setString(1, memberID);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        obj.put("status", "success");
                        obj.put("member_id", rs.getString("Member_ID"));
                        obj.put("current_medication", rs.getString("Curr_Medication"));
                        obj.put("supplement", rs.getString("Supplement"));
                        obj.put("smoke", rs.getString("Smoke"));
                        obj.put("allergy", rs.getString("Allergy"));
                        obj.put("injury", rs.getString("Injury"));
                        obj.put("medical_history", rs.getString("Medical_History"));
                        obj.put("diet_preference", rs.getString("Diet_Preference"));
                        obj.put("drink", rs.getString("Drink"));
                        obj.put("surgery", rs.getString("Surgery"));
                    } else {
                        obj.put("status", "not_found");
                        obj.put("message", "No record found for this member_id");
                    }
                }
            }

        } catch (Exception e) {
            obj.put("status", "error");
            obj.put("message", e.getMessage());
        }

        out.print(obj.toJSONString());
    }
}
