package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

@SuppressWarnings("unchecked")
@WebServlet("/SalaryRecordAPI")
public class SalaryRecordAPI extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String trainerID = request.getParameter("id");

        if (trainerID == null || trainerID.isEmpty()) {
            // return empty array or an error object; here returning empty array
            out.print("[]");
            out.flush();
            return;
        }

        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        JSONArray salaryRecords = new JSONArray();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            pst = con.prepareStatement("SELECT * FROM salary WHERE Trainer_ID = ?");
            pst.setString(1, trainerID);

            rs = pst.executeQuery();

            while (rs.next()) {
                JSONObject record = new JSONObject();
                record.put("salary_id", rs.getString("Salary_ID"));
                record.put("pay_date", rs.getString("Pay_date"));
                record.put("mode", rs.getString("Pay_Mode"));
                record.put("pay_salary_month", rs.getString("Month"));
                record.put("amount", rs.getBigDecimal("Amount"));
                salaryRecords.add(record);
            }

        } catch (Exception e) {
            // on error you might want to return an object instead of array
            JSONObject error = new JSONObject();
            error.put("status", "error");
            error.put("message", e.getMessage());
            out.print(error.toJSONString());
            out.flush();
            return;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pst != null) pst.close();
                if (con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // root is now an array of objects
        out.print(salaryRecords.toJSONString());
        out.flush();
    }
}
