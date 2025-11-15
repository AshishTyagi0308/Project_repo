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
@WebServlet("/Member")
public class Member extends HttpServlet {
	private static final long serialVersionUID = 1L;
    // Database connection configuration
    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "stud102024su"; // change this

    // 🔹 POST method - to add a new member
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String name = request.getParameter("name");
        String gender = request.getParameter("gender");
        String dob = request.getParameter("dob");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASS);

            String sql = "INSERT INTO member (name, gender, dob, phone, address) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, name);
            ps.setString(2, gender);
            ps.setString(3, dob);
            ps.setString(4, phone);
            ps.setString(5, address);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    out.println("{\"message\":\"Member added successfully!\", \"unique_id\":" + id + "}");
                }
            } else {
                out.println("{\"message\":\"Failed to add member.\"}");
            }

            con.close();
        } catch (Exception e) {
            out.println("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    // 🔹 GET method - to fetch member details by ID
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String idParam = request.getParameter("id");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASS);

            String sql = "SELECT * FROM member WHERE id = ? ";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, Integer.parseInt(idParam));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                out.println("{");
                out.println("\"id\": " + rs.getInt("id") + ",");
                out.println("\"name\": \"" + rs.getString("name") + "\",");
                out.println("\"gender\": \"" + rs.getString("gender") + "\",");
                out.println("\"dob\": \"" + rs.getString("dob") + "\",");
                out.println("\"phone\": \"" + rs.getString("phone") + "\",");
                out.println("\"address\": \"" + rs.getString("address") + "\"");
                out.println("}");
            } else {
                out.println("{\"message\":\"Member not found.\"}");
            }

            con.close();
        } catch (Exception e) {
            out.println("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}