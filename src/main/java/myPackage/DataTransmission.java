package myPackage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked")
@WebServlet("/DataTransmission")
public class DataTransmission extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    @SuppressWarnings("unused")
	private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:5173",
            "https://wellness-management-system.vercel.app",
            "https://admonitorial-cinderella-hungerly.ngrok-free.dev"
        );

    private void addCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "864000");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCORSHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        JSONArray membersArray = new JSONArray();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/gym", "root", "Ashish_mca@1234");
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM member")) {

                while (rs.next()) {
                    JSONObject member = new JSONObject();
                    member.put("Member_ID", rs.getInt("Member_ID"));
                    member.put("Name", rs.getString("Name"));
                    member.put("DOB", rs.getDate("DOB") != null ? rs.getDate("DOB").toString() : "");
                    member.put("Gender", rs.getString("Gender"));
                    member.put("Phone_no", rs.getString("Phone_no"));
                    member.put("Address", rs.getString("Address"));
                    membersArray.add(member);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Output array directly
        out.print(membersArray.toJSONString());
        out.flush();
    }
}
