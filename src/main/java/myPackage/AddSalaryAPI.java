package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings("unchecked")
@WebServlet("/AddSalaryAPI")
public class AddSalaryAPI extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject json = new JSONObject();

        String trainerID = request.getParameter("id");

        Connection con = null;
        PreparedStatement pst = null;

        try {
            // Read full JSON body
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

            // ---- validate required fields ----
            List<String> errors = new ArrayList<>();

            if (trainerID == null || trainerID.trim().isEmpty()) {
                errors.add("trainer_id is required");
            }
            if (payDate == null || payDate.trim().isEmpty()) {
                errors.add("pay_date is required");
            }
            if (payMode == null || payMode.trim().isEmpty()) {
                errors.add("mode is required");
            }
            if (month == null || month.trim().isEmpty()) {
                errors.add("pay_salary_month is required");
            }
            if (amountObj == null || amountObj.toString().trim().isEmpty()) {
                errors.add("amount is required");
            }

            if (!errors.isEmpty()) {
                json.put("success", 0);
                json.put("message", "All fields are required.");
                json.put("errors", errors.toString());
                json.put("body", body); // echo back what was sent
                out.print(json.toJSONString());
                out.flush();
                return;
            }

            // ---- parse amount safely ----
            BigDecimal amountBD;
            if (amountObj instanceof Number) {
                amountBD = new BigDecimal(amountObj.toString());
            } else {
                amountBD = new BigDecimal(amountObj.toString().trim());
            }

            // ---- insert into DB ----
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASS);

            pst = con.prepareStatement(
                    "INSERT INTO salary (Trainer_ID, Pay_date, Pay_mode, Month, Amount) VALUES (?,?,?,?,?)"
            );
            pst.setString(1, trainerID);
            pst.setString(2, payDate);
            pst.setString(3, payMode);
            pst.setString(4, month);
            pst.setBigDecimal(5, amountBD);

            int result = pst.executeUpdate();

            if (result > 0) {
                json.put("success", 1);
                json.put("message", "Salary record added successfully");
                json.put("data", body);
                json.put("trainer_id", trainerID);
            } else {
                json.put("success", 0);
                json.put("message", "Failed to add salary record");
            }

        } catch (Exception e) {
            json.put("success", 0);
            json.put("message", "Exception occurred");
            json.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            try {
                if (pst != null) pst.close();
                if (con != null) con.close();
            } catch (SQLException ignore) {}
        }

        out.print(json.toJSONString());
        out.flush();
    }
}
