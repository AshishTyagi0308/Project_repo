package myPackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

@WebServlet("/EditGetSlotAPI")
public class EditGetSlotAPI extends HttpServlet {

    private static final String JDBC_URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "Ashish_mca@1234";

    private static final String SQL =
        "SELECT sr.Start_time, sr.End_time, sr.Label " +
        "FROM trainer_schedule ts " +
        "JOIN slot_record sr ON ts.Slot_ID = sr.Slot_ID " +
        "WHERE ts.Schedule_ID = ?";

    private static class SlotDTO {
        private String start_time;
        private String end_time;
        private String label;

        public SlotDTO(String start_time, String end_time, String label) {
            this.start_time = start_time;
            this.end_time = end_time;
            this.label = label;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // CORS
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Gson gson = new Gson();
        SlotDTO slot = null;

        String scheduleIdParam = request.getParameter("id");

        if (scheduleIdParam != null && !scheduleIdParam.isEmpty()) {
            try {
                int scheduleId = Integer.parseInt(scheduleIdParam);

                Class.forName("com.mysql.cj.jdbc.Driver");

                try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
                     PreparedStatement ps = conn.prepareStatement(SQL)) {

                    ps.setInt(1, scheduleId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String start = rs.getString("Start_time");
                            String end   = rs.getString("End_time");
                            String label = rs.getString("Label");

                            // replace null or empty with "-"
                            start = (start == null || start.trim().isEmpty()) ? "-" : start;
                            end   = (end   == null || end.trim().isEmpty())   ? "-" : end;
                            label = (label == null || label.trim().isEmpty()) ? "-" : label;

                            slot = new SlotDTO(start, end, label);
                        }
                    }
                }
            } catch (NumberFormatException | ClassNotFoundException | SQLException e) {
                // on any error, slot stays null
            }
        }

        // Response is ONLY the SlotDTO (or null) as a single JSON object
        PrintWriter out = response.getWriter();
        out.print(gson.toJson(slot));   // e.g. { "startTime": "...", "endTime": "...", "label": "-" } [web:3][web:7]
        out.flush();
    }
}
