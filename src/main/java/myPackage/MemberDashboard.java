package myPackage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
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
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCORSHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCORSHeaders(response);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/gym", "root", "Ashish_mca@1234");

            String sql =
                    "SELECT m.Member_ID, m.Name, m.Phone_no, " +
                    "       mem.Membership_ID, mem.Membership_type, mem.End_date, mem.Total_fee, " +
                    "       COALESCE(SUM(p.Paid_fee), 0) AS paid_fee " +
                    "FROM member m " +
                    "JOIN ( " +
                    "    SELECT Member_ID, Membership_ID, Membership_type, End_date, Total_fee " +
                    "    FROM membership " +
                    "    WHERE (Member_ID, Membership_ID) IN ( " +
                    "        SELECT Member_ID, MAX(Membership_ID) " +
                    "        FROM membership " +
                    "        GROUP BY Member_ID " +
                    "    ) " +
                    ") mem ON m.Member_ID = mem.Member_ID " +
                    "LEFT JOIN payment p ON mem.Membership_ID = p.Membership_ID " +
                    "GROUP BY m.Member_ID, m.Name, m.Phone_no, " +
                    "         mem.Membership_ID, mem.Membership_type, mem.End_date, mem.Total_fee " +
                    "ORDER BY m.Member_ID;";

            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            JSONArray resultList = new JSONArray();

            while(rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getInt("Member_ID"));
                obj.put("name", rs.getString("Name"));
                obj.put("phone", rs.getString("Phone_no"));
                obj.put("end_date", rs.getDate("End_date").toString());
                obj.put("type", rs.getString("Membership_type"));

                double totalFee = rs.getDouble("Total_fee");
                double paidFee = rs.getDouble("paid_fee");
                String paymentStatus = totalFee == paidFee ? "paid" : "pending";
                obj.put("payment_status", paymentStatus);

                resultList.add(obj);
            }
            out.print(resultList.toString());
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
