package myPackage;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

@SuppressWarnings("serial")
@WebServlet("/DeleteSalaryAPI")
public class DeleteSalaryAPI extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String salaryID = request.getParameter("id");

        // 1. CHECK IF ID IS MISSING
        if (salaryID == null || salaryID.trim().isEmpty()) {
            out.print("{\"status\":\"error\", \"message\":\"salary_id is required\"}");
            return;
        }

        // 2. CHECK IF ID IS NOT NUMERIC
        int id;
        try {
            id = Integer.parseInt(salaryID);
        } catch (NumberFormatException e) {
            out.print("{\"status\":\"error\", \"message\":\"salary_id must be numeric\"}");
            return;
        }

        try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {

            String sql = "DELETE FROM salary WHERE Salary_ID=?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, id);

            int rows = pst.executeUpdate();

            if (rows > 0) {
                out.print("{\"status\":\"success\",\"message\":\"Salary deleted\"}");
            } else {
                out.print("{\"status\":\"failed\",\"message\":\"No record found for this salary_id\"}");
            }

        } catch (Exception e) {
            out.print("{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}");
        }
    }
}
