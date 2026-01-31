package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/DeleteClientAPI")
public class DeleteClientAPI extends HttpServlet {

    // change according to your DB
    private static final String JDBC_URL ="jdbc:mysql://localhost:3306/gym";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "Ashish_mca@1234";

    // delete by Join_ID (primary key); change column if you want by Trainer_ID/Member_ID
    private static final String DELETE_SQL =
        "DELETE FROM trainer_member WHERE Join_ID =?";

    @Override
    protected void doDelete(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String idParam = request.getParameter("id"); // from frontend

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        if (idParam == null || idParam.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"id parameter is required\"}");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"success\":false,\"message\":\"id must be an integer\"}");
            return;
        }

        int rowsDeleted = 0;

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {

            // Class.forName(\"com.mysql.cj.jdbc.Driver\"); // only if your setup needs it

            ps.setInt(1, id);
            rowsDeleted = ps.executeUpdate();    // number of rows removed

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\":false,\"message\":\"database error\"}");
            return;
        }

        // success response
        out.write("{\"success\":true,\"rowsDeleted\":" + rowsDeleted + "}");
    }
}
