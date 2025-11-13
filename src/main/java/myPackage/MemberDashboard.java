package myPackage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@SuppressWarnings("unchecked")
@WebServlet("/MemberDashboard")
public class MemberDashboard extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private void addCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "864000");
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCORSHeaders(response);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONArray membersArray = new JSONArray();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/gym", "root", "Ashish_mca@1234");

            Statement stmt = con.createStatement();
            String query = "SELECT m.member_id, m.member_name, m.phone, mem.membership_type, mem.end_date, mem.total_fees, COALESCE(SUM(p.amount), 0) AS paid_amount, (mem.total_fees - COALESCE(SUM(p.amount), 0)) AS pending_fee FROM member m JOIN membership mem ON m.member_id = mem.member_id LEFT JOIN payment p ON m.member_id = p.member_id GROUP BY m.member_id, m.member_name, m.phone, mem.membership_type, mem.end_date, mem.total_fees";
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                JSONObject memberObj = new JSONObject();
                memberObj.put("member_id", rs.getInt("member_id"));
                memberObj.put("member_name", rs.getString("member_name"));
                memberObj.put("phone", rs.getString("phone"));
                memberObj.put("membership_type", rs.getString("membership_type"));
                memberObj.put("end_date", rs.getDate("end_date").toString());
                memberObj.put("total_fees", rs.getDouble("total_fees"));
                memberObj.put("paid_amount", rs.getDouble("paid_amount"));
                memberObj.put("pending_fee", rs.getDouble("pending_fee"));
                membersArray.add(memberObj);
            }

            out.print(membersArray.toJSONString());
            out.flush();

            rs.close();
            stmt.close();
            con.close();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error occurred\"}");
            out.flush();
        }
    }
}
