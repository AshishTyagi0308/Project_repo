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

@SuppressWarnings("unchecked")
@WebServlet("/TransferDataUser")
public class TransferDataUser extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Add CORS headers helper
    private void addCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "864000"); // cache preflight for 1 day
        // Removed Access-Control-Allow-Credentials since wildcard origin is used
    }

    // Handle GET request
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        addCORSHeaders(response);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        JSONArray usersArray = new JSONArray();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/gym", "root", "Ashish_mca@1234");

            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM user");

            // Debug: check if any row is fetched
            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                JSONObject user = new JSONObject();

                // Use correct case for column names from your DB schema
                user.put("username", rs.getString("Username"));
                user.put("password", rs.getString("Password"));
                user.put("role", rs.getString("Role"));
                user.put("email", rs.getString("Email"));

                usersArray.add(user);
            }

            if(!hasData) {
                System.out.println("No data found in user table.");
            }

            rs.close();
            st.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            // Optional: send error response as JSON object
            JSONObject err = new JSONObject();
            err.put("error", e.getMessage());
            out.print(err.toJSONString());
            out.flush();
            return;
        }

        out.print(usersArray.toJSONString());
        out.flush();
    }

    // Handle OPTIONS request (for CORS preflight)
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCORSHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
