package myPackage;
import java.io.PrintWriter;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

@SuppressWarnings("unchecked")
@WebServlet("/MembershipDetail")
public class MembershipDetail extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    // Helper method to add CORS headers
    private void addCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "864000");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        addCORSHeaders(resp);
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        JSONArray membershipArray = new JSONArray();

        String memberId = req.getParameter("id");

        if (memberId == null || memberId.trim().equals("")) {
            JSONObject error = new JSONObject();
            error.put("error", "id is required");
            out.print(error.toString());
            out.flush();
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement pst = con.prepareStatement(
                         "SELECT Member_ID, Membership_ID, Membership_type, Start_date, End_date, Total_fee FROM membership WHERE Member_ID = ?")) {
                pst.setString(1, memberId);

                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        JSONObject obj = new JSONObject();
                        obj.put("id", rs.getString("Membership_ID"));
                        obj.put("type", rs.getString("Membership_type"));
                        obj.put("start_date", rs.getString("Start_date"));
                        obj.put("end_date", rs.getString("End_date"));
                        obj.put("fees", rs.getString("Total_fee"));
                        membershipArray.add(obj);
                    }
                }
            }

            out.print(membershipArray.toString());
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
            JSONObject error = new JSONObject();
            error.put("error", "Server Error");
            out.print(error.toString());
            out.flush();
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        addCORSHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
