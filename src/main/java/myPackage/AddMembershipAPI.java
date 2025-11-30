package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@WebServlet("/AddMembershipAPI")
public class AddMembershipAPI extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        JSONObject obj = new JSONObject();

        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = req.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonInput = (JSONObject) parser.parse(sb.toString());

            // Read input fields (id = Member_ID from param)
            String id = req.getParameter("id");
            String type = (String) jsonInput.get("type");
            String startDate = (String) jsonInput.get("start_date");
            String endDate = (String) jsonInput.get("end_date");
            String amount = (String) jsonInput.get("amount");

            // Validation
            if (id == null || id.trim().isEmpty() || type == null || type.trim().isEmpty() ||
                startDate == null || startDate.trim().isEmpty() ||
                endDate == null || endDate.trim().isEmpty() ||
                amount == null || amount.trim().isEmpty()) {
                obj.put("success", false);
                obj.put("message", "All fields are required.");
                out.print(obj.toJSONString());
                out.flush();
                return;
            }

            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
                String sql = "INSERT INTO membership (Member_ID, Membership_type, Start_date, End_date, Total_fee) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pst = con.prepareStatement(sql)) {
                    pst.setString(1, id);
                    pst.setString(2, type);
                    pst.setString(3, startDate);
                    pst.setString(4, endDate);
                    pst.setString(5, amount);

                    int rowsInserted = pst.executeUpdate();
                    if (rowsInserted > 0) {
                        obj.put("success", true);
                        obj.put("message", "Membership added successfully.");
                    } else {
                        obj.put("success", false);
                        obj.put("message", "Failed to add membership.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            obj.put("success", false);
            obj.put("message", "Internal server error: " + e.getMessage());
        }

        out.print(obj.toJSONString());
        out.flush();
    }
}
