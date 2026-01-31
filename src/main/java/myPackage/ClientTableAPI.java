package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/ClientTableAPI")
public class ClientTableAPI extends HttpServlet {

    // DB config
    private static final String JDBC_URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "Ashish_mca@1234";

    private static final String QUERY =
        "SELECT tm.Join_ID, m.Name AS member_name " +
        "FROM trainer_member tm " +
        "JOIN member m ON tm.Member_ID = m.Member_ID " +
        "WHERE tm.Trainer_ID = ?";

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        // Read Trainer_ID from param "id"
        String trainerIdParam = request.getParameter("id");
        if (trainerIdParam == null || trainerIdParam.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter()
                    .write("{\"error\":\"Trainer_ID parameter is required\"}");
            return;
        }

        int trainerId;
        try {
            trainerId = Integer.parseInt(trainerIdParam);
        } catch (NumberFormatException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter()
                    .write("{\"error\":\"Trainer_ID must be an integer\"}");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }

        List<String> rows = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
             PreparedStatement ps = conn.prepareStatement(QUERY)) {

            ps.setInt(1, trainerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int joinId = rs.getInt("Join_ID");
                    String memberName = rs.getString("member_name");

                    String obj = String.format(
                        "{\"join_id\":%d,\"member_name\":\"%s\"}",
                        joinId,
                        escapeJson(memberName)
                    );
                    rows.add(obj);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter()
                    .write("{\"error\":\"Database error\"}");
            return;
        }

        // Build JSON array: [ {...}, {...} ]
        StringBuilder json = new StringBuilder();
        json.append("[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) json.append(",");
            json.append(rows.get(i));
        }
        json.append("]");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.write(json.toString());
        out.flush();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"");
    }
}
