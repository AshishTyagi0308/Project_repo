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

@WebServlet("/MemberDetail/*") // Note the wildcard to support MemberDetail/{memberID}
public class MemberDetail extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234"; // change this

    // Set dynamic CORS header for trusted origins only
    private void addCORSHeaders(HttpServletResponse response, HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && (origin.contains("localhost") || origin.contains("ngrok-free.dev"))) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, ngrok-skip-browser-warning");
        response.setHeader("Access-Control-Max-Age", "864000");
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCORSHeaders(response, request);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    // Helper to parse ID from either path or query
    private Integer getMemberId(HttpServletRequest request) {
        // Try query string first (for /MemberDetail?id=500)
        String idParam = request.getParameter("id");
        if (idParam != null) {
            try {
                return Integer.parseInt(idParam);
            } catch (NumberFormatException e) { return null; }
        }
        // Try path info for REST style (/MemberDetail/500)
        String pathInfo = request.getPathInfo(); // returns "/500"
        if (pathInfo != null && pathInfo.length() > 1) {
            try {
                return Integer.parseInt(pathInfo.substring(1));
            } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCORSHeaders(response, request);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        Integer memberId = getMemberId(request);
        if (memberId == null) {
            out.println("{\"error\":\"Invalid or missing member ID.\"}");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASS);

            String sql = "SELECT * FROM member WHERE Member_ID = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, memberId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                JSONObject member = new JSONObject();
                member.put("name", rs.getString("Name"));
                member.put("gender", rs.getString("Gender"));
                member.put("dob", rs.getString("DOB"));
                member.put("phone", rs.getString("Phone_no"));
                member.put("address", rs.getString("Address"));
                out.println(member.toJSONString());
            } else {
                out.println("{\"message\":\"Member not found.\"}");
            }
            con.close();
        } catch (Exception e) {
            addCORSHeaders(response, request); // Ensure CORS on error too!
            out.println("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}