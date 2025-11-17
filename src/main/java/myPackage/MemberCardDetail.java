package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet("/MemberCardDetail")
public class MemberCardDetail extends HttpServlet {

private static final String URL = "jdbc:mysql://localhost:3306/gym";  
private static final String USER = "root";  
private static final String PASS = "Ashish_mca@1234";  

@Override  
protected void doGet(HttpServletRequest req, HttpServletResponse resp)  
        throws ServletException, IOException {  

    resp.setContentType("application/json");  
    PrintWriter out = resp.getWriter();  

    JSONObject obj = new JSONObject();   // ONLY ONE OBJECT  

    try {  
        Class.forName("com.mysql.jdbc.Driver");  
        Connection con = DriverManager.getConnection(URL, USER, PASS);  

        // TOTAL MEMBERS  
        String totalQuery = "SELECT COUNT(*) FROM membership";  
        PreparedStatement ps1 = con.prepareStatement(totalQuery);  
        ResultSet rs1 = ps1.executeQuery();  
        rs1.next();  
        obj.put("total_members", rs1.getInt(1));  

        // ACTIVE MEMBERS  
        String activeQuery = "SELECT COUNT(*) FROM membership WHERE end_date >= CURDATE()";  
        PreparedStatement ps2 = con.prepareStatement(activeQuery);  
        ResultSet rs2 = ps2.executeQuery();  
        rs2.next();  
        obj.put("active_members", rs2.getInt(1));  

        // EXPIRED MEMBERS  
        String expiredQuery = "SELECT COUNT(*) FROM membership WHERE end_date < CURDATE()";  
        PreparedStatement ps3 = con.prepareStatement(expiredQuery);  
        ResultSet rs3 = ps3.executeQuery();  
        rs3.next();  
        obj.put("expired_members", rs3.getInt(1));  

        // SEND ONLY ONE OBJECT  
        out.print(obj);  

        con.close();  

    } catch (Exception e) {  
        e.printStackTrace();  
    }  
}

}