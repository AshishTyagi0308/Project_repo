package myPackage;

import org.json.simple.JSONObject;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.*;

@SuppressWarnings("unchecked")
@WebServlet("/EditGetMembershipAPI")
public class EditGetMembershipAPI extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";   // change password

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject obj = new JSONObject();
        
        String id = request.getParameter("id");

        if (id == null || id.trim().isEmpty()) {
            obj.put("status", "failed");
            obj.put("message", "Membership ID missing");
            out.print(obj);
            return;
        }
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASS);
            
            String query = "SELECT Membership_type, Start_date, End_date, Total_fee FROM membership WHERE Membership_ID = ?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, id);
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                //obj.put("status", "success");
                obj.put("type", rs.getString("Membership_type"));          // pt or general
                obj.put("start_date", rs.getString("Start_date"));
                obj.put("end_date", rs.getString("End_date"));
                obj.put("amount", rs.getString("Total_fee"));
            } else {
                obj.put("status", "failed");
                obj.put("message", "No membership found");
            }
            
            con.close();
        } catch (Exception e) {
            obj.put("status", "error");
            obj.put("message", e.getMessage());
        }
        
        out.print(obj);
    }
}