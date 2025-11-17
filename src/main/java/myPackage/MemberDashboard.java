package myPackage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/MemberDashboard")
public class MemberDashboard extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private void addCORS(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        response.setHeader("Access-Control-Allow-Origin", origin != null ? origin : "*");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Vary", "Origin");
        response.setHeader("Access-Control-Max-Age", "86400");
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCORS(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCORS(request, response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        JSONArray resultList = new JSONArray();

        String url = "jdbc:mysql://localhost:3306/gym";
        String user = "root";
        String pass = "Ashish_mca@1234";
        String query =
            "SELECT m.Member_ID, m.Name, m.Phone_no, " +
            "mem.Membership_ID, mem.Membership_type, mem.End_date, mem.Total_fee, " +
            "COALESCE(SUM(p.Paid_fee), 0) AS paid_fee " +
            "FROM member m " +
            "LEFT JOIN (SELECT Member_ID, Membership_ID, Membership_type, End_date, Total_fee " +
                       "FROM membership WHERE (Member_ID, Membership_ID) IN " +
                       "(SELECT Member_ID, MAX(Membership_ID) FROM membership GROUP BY Member_ID)) mem " +
            "ON m.Member_ID = mem.Member_ID " +
            "LEFT JOIN payment p ON mem.Membership_ID = p.Membership_ID " +
            "GROUP BY m.Member_ID, m.Name, m.Phone_no, mem.Membership_ID, mem.Membership_type, mem.End_date, mem.Total_fee " +
            "ORDER BY m.Member_ID;";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            addCORS(request, response);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject err = new JSONObject();
            err.put("error", "MySQL JDBC Driver not found. Add mysql-connector-java.jar to WEB-INF/lib. " + e.getMessage());
            out.print(err.toJSONString());
            out.flush();
            return;
        }

        try (Connection con = DriverManager.getConnection(url, user, pass);
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getInt("Member_ID"));
                obj.put("name", rs.getString("Name"));
                obj.put("phone", rs.getString("Phone_no"));
                Date endDate = rs.getDate("End_date");
                obj.put("end_date", endDate != null ? endDate.toString() : "");
                obj.put("type", rs.getString("Membership_type") != null ? rs.getString("Membership_type") : "-");
                obj.put("membership_id", rs.getString("Membership_ID") != null ? rs.getString("Membership_ID") : "");
                double total = rs.getDouble("Total_fee");
                double paid = rs.getDouble("paid_fee");

                // Set payment_status based on membership existence
                if (rs.getString("Membership_ID") == null) {
                    obj.put("payment_status", "-"); // no membership, status is null
                } else {
                    obj.put("payment_status", total == paid ? "paid" : "pending");
                }
                resultList.add(obj);
            }
            out.print(resultList.toJSONString());
        } catch (SQLException e) {
            addCORS(request, response);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject err = new JSONObject();
            err.put("error", "SQL Error: " + e.getMessage());
            out.print(err.toJSONString());
        } finally {
            out.flush();
        }
    }
}
