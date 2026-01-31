package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

@WebServlet("/EditGetHealthAPI")
public class EditGetHealthAPI extends HttpServlet {

    private static final String URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleGetHealth(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleGetHealth(req, resp);
    }

    private void handleGetHealth(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        JSONObject obj = new JSONObject();

        String member_id = req.getParameter("id");

        if (member_id == null || member_id.isEmpty()) {
            obj.put("status", "failed");
            obj.put("message", "member_id is required");
            out.print(obj);
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASS);

            String sql = "SELECT Member_ID, Medical_history, Curr_Medication, Allergy, Surgery, Injury, "
                       + "Supplement, Diet_Preference, Drink, Smoke "
                       + "FROM health_records WHERE Member_ID = ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, member_id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                obj.put("status", "success");
                obj.put("member_id", member_id);
                obj.put("medical_history",     valueOrDash(rs.getString("Medical_history")));
                obj.put("current_medication",  valueOrDash(rs.getString("Curr_Medication")));
                obj.put("allergy",             valueOrDash(rs.getString("Allergy")));
                obj.put("surgery",             valueOrDash(rs.getString("Surgery")));
                obj.put("injury",              valueOrDash(rs.getString("Injury")));
                obj.put("supplement",          valueOrDash(rs.getString("Supplement")));
                obj.put("diet_preference",     valueOrDash(rs.getString("Diet_Preference")));
                obj.put("drink",               valueOrDash(rs.getString("Drink")));
                obj.put("smoke",               valueOrDash(rs.getString("Smoke")));
            } else {
                obj.put("status", "failed");
                obj.put("message", "No health record found for this member_id");
            }

            out.print(obj);
            con.close();

        } catch (Exception e) {
            obj.put("status", "failed");
            obj.put("message", e.getMessage());
            out.print(obj);
        }
    }

    // Helper: convert null or empty string to "-"
    private String valueOrDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s;
    }
}
