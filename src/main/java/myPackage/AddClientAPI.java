package myPackage;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@WebServlet("/AddClientAPI")
public class AddClientAPI extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Change these according to your DB
    private static final String JDBC_URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "Ashish_mca@1234";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // trainerId from query parameter
        String trainerIdParam = request.getParameter("trainer_id");
        if (trainerIdParam == null || trainerIdParam.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"trainerId parameter is required\"}");
            return;
        }

        int trainerId;
        try {
            trainerId = Integer.parseInt(trainerIdParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"trainerId must be a number\"}");
            return;
        }

        // Read JSON body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {   // read POST body as text[web:38]
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        if (sb.length() == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"JSON body is required\"}");
            return;
        }

        // Parse JSON using json-simple
        JSONObject jsonBody;
        try {
            JSONParser parser = new JSONParser();             // json-simple parser[web:24][web:49]
            jsonBody = (JSONObject) parser.parse(sb.toString());
        } catch (ParseException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"Invalid JSON format\"}");
            return;
        }

        String memberName = (String) jsonBody.get("name");    // get values by key[web:24][web:30]
        String phoneNo    = (String) jsonBody.get("phone");

        if (memberName == null || memberName.isEmpty()
                || phoneNo == null || phoneNo.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"All fields are required.\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement psFind = null;
        PreparedStatement psInsert = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");        // MySQL driver[web:45]
            conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);

            // 1) Find member by name and phone
            String findSql = "SELECT Member_ID FROM member WHERE Name = ? AND Phone_no = ?";
            psFind = conn.prepareStatement(findSql);          // PreparedStatement for parameters[web:44]
            psFind.setString(1, memberName);
            psFind.setString(2, phoneNo);
            rs = psFind.executeQuery();

            if (!rs.next()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"success\":false,\"message\":\"Member not found\"}");
                return;
            }

            int memberId = rs.getInt("Member_ID");

            // 2) Insert mapping into trainer_member
            String insertSql = "INSERT INTO trainer_member (Trainer_ID, Member_ID) VALUES (?, ?)";
            psInsert = conn.prepareStatement(insertSql);
            psInsert.setInt(1, trainerId);
            psInsert.setInt(2, memberId);
            int rows = psInsert.executeUpdate();              // execute insert[web:45]

            if (rows > 0) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.write("{\"success\":true,\"message\":\"Client assigned successfully\",\"trainerId\":" 
                        + trainerId + ",\"memberId\":" + memberId + "}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("{\"success\":false,\"message\":\"Failed to assign client\"}");
            }

        } catch (ClassNotFoundException | SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\":false,\"message\":\"Server error\"}");
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (psFind != null) psFind.close(); } catch (SQLException ignored) {}
            try { if (psInsert != null) psInsert.close(); } catch (SQLException ignored) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
        }
    }
}
