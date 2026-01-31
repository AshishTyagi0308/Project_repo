package myPackage;

import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import java.sql.*;
import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
@WebServlet("/TrainerCardDetailAPI")
public class TrainerCardDetailAPI extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject obj = new JSONObject();

        int total = 0;
        int active = 0;
        int inactive = 0;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASS);

            // Query 1: Total Trainer Count
            String q1 = "SELECT COUNT(*) AS total FROM trainer";
            PreparedStatement ps1 = con.prepareStatement(q1);
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) total = rs1.getInt("total");

            // Query 2: Active Trainer Count
            String q2 = "SELECT COUNT(*) AS active FROM trainer WHERE Status='Active'";
            PreparedStatement ps2 = con.prepareStatement(q2);
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) active = rs2.getInt("active");

            // Query 3: Inactive Trainer Count
            String q3 = "SELECT COUNT(*) AS inactive FROM trainer WHERE Status='Inactive'";
            PreparedStatement ps3 = con.prepareStatement(q3);
            ResultSet rs3 = ps3.executeQuery();
            if (rs3.next()) inactive = rs3.getInt("inactive");

            con.close();

            obj.put("total_trainers", total);
            obj.put("active_trainers", active);
            obj.put("inactive_trainers", inactive);
            obj.put("message", "Success");

        } catch (Exception e) {
            obj.put("error", e.toString());
        }

        out.print(obj);
    }
}