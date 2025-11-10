package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.*;
@SuppressWarnings("unchecked")
public class DataTransmission extends HttpServlet {
	private static final long serialVersionUID = 1L;
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        JSONArray membersArray = new JSONArray();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/gym", "root", "yourpassword");

            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM member");

            while (rs.next()) {
                JSONObject member = new JSONObject();
                member.put("Member_ID", rs.getInt("Member_ID"));
                member.put("Name", rs.getString("Name"));
                member.put("DOB", rs.getDate("DOB").toString());
                member.put("Gender", rs.getString("Gender"));
                member.put("Phone_no", rs.getString("Phone_no"));
                member.put("Address", rs.getString("Address"));
                membersArray.add(member);
            }

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        out.print(membersArray.toJSONString());
    }
}