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
    private static final String PASS = "Ashish_mca@1234";

    private void setCorsHeaders(HttpServletResponse resp, HttpServletRequest req) {
        String origin = req.getHeader("Origin");
        if (origin != null && (origin.contains("localhost") || origin.contains("ngrok-free.dev"))) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Vary", "Origin");
        }
        resp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Max-Age", "86400");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp, req);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp, req);
        resp.setContentType("application/json");
        JSONArray  memberships = new JSONArray();

        String memberId = req.getParameter("id");
        if (memberId == null || memberId.trim().isEmpty()) {
            JSONObject error = new JSONObject();
            error.put("error","id is required");
            resp.getWriter().print(error.toString());
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement pst = con.prepareStatement(
                         "SELECT Member_ID, Membership_ID, Membership_type, Start_date, End_date, Total_fee FROM membership WHERE Member_ID = ?")) {
                pst.setString(1, memberId);

                try (ResultSet rs = pst.executeQuery()) {
                    while(rs.next()) {
                        JSONObject obj = new JSONObject();
                        obj.put("id", rs.getString("Membership_ID"));
                        obj.put("type", rs.getString("Membership_type"));
                        obj.put("start_date", rs.getString("Start_date"));
                        obj.put("end_date", rs.getString("End_date"));
                        obj.put("fees", rs.getString("Total_fee"));
                        memberships.add(obj);
                    } 
                    con.close();
                        resp.getWriter().print(memberships.toString());
                    }
                }
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject error = new JSONObject();
            error.put("error", "Server Error");
            resp.getWriter().print(error.toString());
        }
    }
}
