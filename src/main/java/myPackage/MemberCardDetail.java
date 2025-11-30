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

import org.json.simple.JSONObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

@WebServlet("/MemberCardDetail")
public class MemberCardDetail extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/gym";  
    private static final String USER = "root";  
    private static final String PASS = "Ashish_mca@1234";  

    // JWT validation with error reporting
    private boolean isTokenValid(String token, HttpServletResponse response) throws IOException {
        try {
            Jwts.parser()
                .setSigningKey("RaJdNoqNevTsnjh9Vgbe/LgPCrbcjwTCfKWpBuOyPTM=".getBytes())
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            sendError(response, "Invalid token signature");
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            sendError(response, "Token expired");
            return false;
        } catch (Exception e) {
            sendError(response, "Malformed or invalid token: " + e.getMessage());
            return false;
        }
    }

    // Helper method for sending JSON errors
    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject err = new JSONObject();
        err.put("error", message);
        out.print(err.toJSONString());
        out.flush();
    }

    @Override  
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)  
            throws ServletException, IOException {  

        resp.setContentType("application/json");  
        PrintWriter out = resp.getWriter();  
        
        // Authentication: Check Authorization header
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(resp, "Unauthorized: Missing or invalid Authorization header.");
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        if (!isTokenValid(token, resp)) {
            return;
        }

        JSONObject obj = new JSONObject();   

        try {  
            Class.forName("com.mysql.jdbc.Driver");  
            Connection con = DriverManager.getConnection(URL, USER, PASS);  

            // TOTAL MEMBERS (only those with membership records) - active + expired
            String totalQuery = """
                SELECT COUNT(DISTINCT m.member_id) 
                FROM member m 
                INNER JOIN membership mem ON m.member_id = mem.member_id
            """;
            PreparedStatement ps1 = con.prepareStatement(totalQuery);  
            ResultSet rs1 = ps1.executeQuery();  
            rs1.next();  
            obj.put("total_members", rs1.getInt(1));  

            // ACTIVE MEMBERS: Members whose LATEST membership end_date >= CURDATE()
            String activeQuery = """
                SELECT COUNT(DISTINCT m.member_id) 
                FROM member m 
                INNER JOIN membership mem ON m.member_id = mem.member_id 
                WHERE mem.end_date = (
                    SELECT MAX(mem2.end_date) 
                    FROM membership mem2 
                    WHERE mem2.member_id = m.member_id
                ) 
                AND mem.end_date >= CURDATE()
            """;
            PreparedStatement ps2 = con.prepareStatement(activeQuery);  
            ResultSet rs2 = ps2.executeQuery();  
            rs2.next();  
            obj.put("active_members", rs2.getInt(1));  

            // EXPIRED MEMBERS: Members whose LATEST membership end_date < CURDATE()
            String expiredQuery = """
                SELECT COUNT(DISTINCT m.member_id) 
                FROM member m 
                INNER JOIN membership mem ON m.member_id = mem.member_id 
                WHERE mem.end_date = (
                    SELECT MAX(mem2.end_date) 
                    FROM membership mem2 
                    WHERE mem2.member_id = m.member_id
                ) 
                AND mem.end_date < CURDATE()
            """;
            PreparedStatement ps3 = con.prepareStatement(expiredQuery);  
            ResultSet rs3 = ps3.executeQuery();  
            rs3.next();  
            obj.put("expired_members", rs3.getInt(1));  

            // SEND ONLY ONE OBJECT  
            out.print(obj);  
            out.flush();

            con.close();  

        } catch (Exception e) {  
            e.printStackTrace();
            JSONObject errObj = new JSONObject();
            errObj.put("error", "Database query failed: " + e.getMessage());
            out.print(errObj.toJSONString());
            out.flush();
        }  
    }
}
