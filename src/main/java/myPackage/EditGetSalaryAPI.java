package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.*;
import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
@WebServlet("/EditGetSalaryAPI")
public class EditGetSalaryAPI extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/gym"; 
    private static final String USER = "root"; 
    private static final String PASS = "Ashish_mca@1234"; 

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject json = new JSONObject();

        String trainerID = request.getParameter("id");

        if (trainerID == null || trainerID.trim().isEmpty()) {
            json.put("error", "Trainer_ID is required");
            out.print(json);
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASS);

            PreparedStatement pst = con.prepareStatement(
                "SELECT * FROM salary WHERE Salary_ID=?"
            );

            pst.setString(1, trainerID);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                json.put("salary_id", rs.getInt("Salary_ID"));
                json.put("pay_date", rs.getString("Pay_date"));
                json.put("mode", rs.getString("Pay_mode"));
                json.put("pay_salary_month", rs.getString("Month"));
                json.put("amount", rs.getString("Amount"));
            } else {
                json.put("message", "No record found");
                json.put("status", "fail");
            }

            con.close();

        } catch (Exception e) {
            json.put("error", e.getMessage());
        }

        out.print(json);
    }
}