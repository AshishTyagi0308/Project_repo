package myPackage;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
@WebServlet("/NotificationAPI")
public class NotificationAPI extends HttpServlet {

    private static final String URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASS);

            String sql =
                "SELECT m.Member_ID, " +
                "       m.Name, " +
                "       mem.End_date   AS memEnd_date, " +
                "       mem.Total_fee  AS totalFee, " +
                "       COALESCE(p.Paid_fee, 0) AS paidFee, " +
                "       p.Due_date     AS paymentDueDate " +
                "FROM member m " +
                "LEFT JOIN membership mem " +
                "  ON m.Member_ID = mem.Member_ID " +
                "  AND mem.Membership_ID = ( " +
                "        SELECT MAX(m2.Membership_ID) " +
                "        FROM membership m2 " +
                "        WHERE m2.Member_ID = m.Member_ID " +
                "      ) " +
                "LEFT JOIN payment p " +
                "  ON mem.Membership_ID = p.Membership_ID " +
                "  AND p.Payment_ID = ( " +
                "        SELECT MAX(p2.Payment_ID) " +
                "        FROM payment p2 " +
                "        WHERE p2.Membership_ID = mem.Membership_ID " +
                "      )";

            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            JSONArray notifications = new JSONArray();
            LocalDate today = LocalDate.now();

            while (rs.next()) {
                int memberId = rs.getInt("Member_ID");
                String memberName = rs.getString("Name");

                java.sql.Date memEndSql = rs.getDate("memEnd_date");
                LocalDate membershipEnd =
                        (memEndSql != null) ? memEndSql.toLocalDate() : null;

                double totalFee = rs.getDouble("totalFee");
                double paidFee  = rs.getDouble("paidFee");

                java.sql.Date payDueSql = rs.getDate("paymentDueDate");
                LocalDate paymentDueDate =
                        (payDueSql != null) ? payDueSql.toLocalDate() : null;

                // membership status
                boolean hasMembershipEnd = membershipEnd != null;
                boolean isMembershipExpired =
                        hasMembershipEnd && today.isAfter(membershipEnd);
                boolean isRecentlyExpired =
                        isMembershipExpired &&
                        !today.isAfter(membershipEnd.plusDays(10));   // m = -10
                boolean isLongExpired =
                        hasMembershipEnd && today.isAfter(membershipEnd.plusDays(10)); // m = +10

                // payment status
                boolean hasPaymentDue = paymentDueDate != null;
                boolean isPaymentExpired =
                        hasPaymentDue && paymentDueDate.isBefore(today);   // past due
                boolean isPaymentRecentlyExpired =
                        isPaymentExpired &&
                        !today.isAfter(paymentDueDate.plusDays(10));       // p = -10
                boolean isPaymentLongExpired =
                        hasPaymentDue && today.isAfter(paymentDueDate.plusDays(10)); // p = +10

                boolean isPaymentDue =
                        isPaymentExpired && paidFee < totalFee;           // still money pending

                // 1) m = +10, p = +10  -> nothing
                if (isLongExpired && isPaymentLongExpired && isPaymentDue) {
                    continue;
                }

                // 2) m = +10, p = -10 -> only payment
                if (isLongExpired && isPaymentRecentlyExpired && isPaymentDue) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", memberId);
                    obj.put("name", memberName);
                    obj.put("membershipEndDate", null);
                    obj.put("paymentDueDate", paymentDueDate.toString());
                    notifications.add(obj);
                    continue;
                }

                // 3) m = -10, p = -10 -> both
                if (isRecentlyExpired && isPaymentRecentlyExpired && isPaymentDue) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", memberId);
                    obj.put("name", memberName);
                    obj.put("membershipEndDate", membershipEnd.toString());
                    obj.put("paymentDueDate", paymentDueDate.toString());
                    notifications.add(obj);
                    continue;
                }

                // 4) m not expired, p = -10 -> only payment
                boolean isMembershipActive =
                        hasMembershipEnd && !today.isAfter(membershipEnd);
                if (isMembershipActive && isPaymentRecentlyExpired && isPaymentDue) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", memberId);
                    obj.put("name", memberName);
                    obj.put("membershipEndDate", null);
                    obj.put("paymentDueDate", paymentDueDate.toString());
                    notifications.add(obj);
                    continue;
                }

                // 5) m = -10, p not expired or no due -> only membership
                if (isRecentlyExpired && (!hasPaymentDue || !isPaymentExpired)) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", memberId);
                    obj.put("name", memberName);
                    obj.put("membershipEndDate", membershipEnd.toString());
                    obj.put("paymentDueDate", null);
                    notifications.add(obj);
                    continue;
                }

                // 6) active membership & payment not due/no due -> nothing
                if (isMembershipActive && (!hasPaymentDue || !isPaymentDue)) {
                    continue;
                }

                // 7) long‑expired membership & payment not due/no due -> nothing
                if (isLongExpired && (!hasPaymentDue || !isPaymentDue)) {
                    continue;
                }

                // Fallback (should rarely hit) – include whatever info exists
                JSONObject obj = new JSONObject();
                obj.put("id", memberId);
                obj.put("name", memberName);
                obj.put("membershipEndDate",
                        membershipEnd != null ? membershipEnd.toString() : null);
                obj.put("paymentDueDate",
                        paymentDueDate != null ? paymentDueDate.toString() : null);
                notifications.add(obj);
            }

            resp.setContentType("application/json");
            resp.getWriter().write(notifications.toString());

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json");
            resp.getWriter()
                .write("{\"error\":\"Database error: " + e.getMessage() + "\"}");
            e.printStackTrace();
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"Server error\"}");
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException ignore) {}
            }
            if (pstmt != null) {
                try { pstmt.close(); } catch (SQLException ignore) {}
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignore) {}
            }
        }
    }
}
