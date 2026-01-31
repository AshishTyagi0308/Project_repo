package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings("unchecked")
@WebServlet("/EditSalaryAPI")
public class EditSalaryAPI extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject json = new JSONObject();

        // This id is Salary_ID in DB
        String salaryID = request.getParameter("id");

        if (salaryID == null || salaryID.trim().isEmpty()) {
            json.put("success", 0);
            json.put("message", "Salary_ID (id) is required");
            out.print(json.toJSONString());
            return;
        }

        Connection con = null;
        PreparedStatement pst = null;

        try {
            // Read full JSON body into String
            StringBuilder sb = new StringBuilder();
            BufferedReader br = request.getReader();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse(sb.toString());

            String payDate = (String) body.get("pay_date");
            String payMode = (String) body.get("mode");
            String month   = (String) body.get("pay_salary_month");
            Object amountObj = body.get("amount");

            // Basic validation
            if (payDate == null || payMode == null || month == null || amountObj == null) {
                json.put("success", 0);
                json.put("message", "All fields (pay_date, mode, pay_salary_month, amount) are required");
                json.put("body", body);
                out.print(json.toJSONString());
                return;
            }

            BigDecimal amountBD;
            if (amountObj instanceof Number) {
                amountBD = new BigDecimal(amountObj.toString());
            } else {
                amountBD = new BigDecimal(amountObj.toString().trim());
            }

            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            pst = con.prepareStatement(
                "UPDATE salary SET Pay_date = ?, Pay_mode = ?, Month = ?, Amount = ? WHERE Salary_ID = ?"
            );

            pst.setString(1, payDate);
            pst.setString(2, payMode);
            pst.setString(3, month);
            pst.setBigDecimal(4, amountBD);
            pst.setString(5, salaryID);

            int result = pst.executeUpdate();

            if (result > 0) {
                json.put("success", 1);
                json.put("message", "Salary updated successfully");
                json.put("updated", 1);
            } else {
                json.put("success", 0);
                json.put("message", "No record found for given Salary_ID");
                json.put("updated", 0);
            }

        } catch (Exception e) {
            json.put("success", 0);
            json.put("message", "All fields are required.");
            json.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            try {
                if (pst != null) pst.close();
                if (con != null) con.close();
            } catch (SQLException ignore) {}
        }

        out.print(json.toJSONString());
    }
}
