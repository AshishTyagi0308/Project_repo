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

@WebServlet("/DeleteHealthRecordAPI")
public class DeleteHealthRecordAPI extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        JSONObject obj = new JSONObject();

        try {
            // ─── Get member_id from request ───────────────────────────
            String memberID = request.getParameter("member_id");

            if (memberID == null || memberID.trim().isEmpty()) {
                obj.put("status", "error");
                obj.put("message", "member_id is required");
                out.print(obj);
                return;
            }

            // ─── Database Connection ─────────────────────────────────
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASS);

            // ─── Prepare Delete Query ───────────────────────────────
            String query = "DELETE FROM health_records WHERE Member_ID = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, memberID);

            int row = ps.executeUpdate();

            // ─── Set Response Object ────────────────────────────────
            if (row > 0) {
                obj.put("status", "success");
                obj.put("message", "Health record deleted successfully");
                obj.put("member_id", memberID);
            } else {
                obj.put("status", "not_found");
                obj.put("message", "No record found for this member_id");
            }

            ps.close();
            conn.close();

        } catch (Exception e) {
            obj.put("status", "error");
            obj.put("message", e.getMessage());
        }

        out.print(obj);
    }
}