package myPackage;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.sql.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

@SuppressWarnings("unchecked")
@WebServlet("/MembershipDetail")
public class MembershipDetail extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "stud102024su";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        JSONArray membershipArray = new JSONArray();

        // Input: Member_ID (not membership id)
       // String memberId = req.getParameter("id");

       /* if (memberId == null || memberId.trim().equals("")) {
            JSONObject error = new JSONObject();
            error.put("error", "id is required");
            resp.getWriter().print(error.toString());
            return;
        }*/

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection con = DriverManager.getConnection(URL, USER, PASS);

            // SQL logic: Fetch membership details belonging to a member
            String sql = "SELECT Membership_ID, Membership_type, Start_date, End_date, Total_fee " +
                         "FROM membership WHERE Member_ID = 500";

            PreparedStatement pst = con.prepareStatement(sql);
           // pst.setString(1, memberId);

            ResultSet rs = pst.executeQuery();

            // Build JSON array of objects
            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getString("Membership_ID"));
                obj.put("type", rs.getString("Membership_type"));
                obj.put("start_date", rs.getString("Start_date"));
                obj.put("end_date", rs.getString("End_date"));
                obj.put("fees", rs.getString("Total_fee"));

                membershipArray.add(obj);
            }

            con.close();

            resp.getWriter().print(membershipArray.toString());

        } catch (Exception e) {
            e.printStackTrace();
            JSONObject error = new JSONObject();
            error.put("error", "Server Error");
            resp.getWriter().print(error.toString());
        }
    }
}