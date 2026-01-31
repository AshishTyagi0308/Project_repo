package myPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

@WebServlet("/LoginPasswordAPI")
public class LoginPasswordAPI extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // DB config – UPDATE with your MySQL credentials
    private static final String JDBC_URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASS = "Ashish_mca@1234";

    // Email (SMTP) config – CRITICAL: Use Gmail App Password (not regular password)
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";  // FIXED: Was empty!
    private static final String SMTP_USER = "gopalnavetia@gmail.com";   // Your sender email
    // CHANGE THIS: Generate App Password at https://myaccount.google.com/apppasswords
    private static final String SMTP_PASS = "your_16char_app_password_here"; 

    private Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
    }

    private void addCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCorsHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        addCorsHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Gson gson = new Gson();
        JsonObject jsonResponse = new JsonObject();

        // Read JSON body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String username = null;
        // FIXED: Use Gson.fromJson instead of deprecated JsonParser
        try {
            JsonObject body = gson.fromJson(sb.toString(), JsonObject.class);
            if (body.has("username") && !body.get("username").isJsonNull()) {
                username = body.get("username").getAsString().trim();
            }
        } catch (JsonSyntaxException e) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Invalid JSON input");
            writeJson(response, gson, jsonResponse);
            return;
        }

        if (username == null || username.isEmpty()) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Username is required");
            writeJson(response, gson, jsonResponse);
            return;
        }

        // Lookup user and send password to email
        String sql = "SELECT Password, Email FROM user WHERE Username = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "User not found");
                } else {
                    String password = rs.getString("Password");
                    String email    = rs.getString("Email");

                    // FIXED: Add null/empty checks before sending email
                    if (email != null && password != null && !email.trim().isEmpty()) {
                        boolean emailSent = sendPasswordEmail(email.trim(), username, password);
                        if (emailSent) {
                            jsonResponse.addProperty("success", true);
                            jsonResponse.addProperty("message", "Password sent to registered email");
                        } else {
                            jsonResponse.addProperty("success", false);
                            jsonResponse.addProperty("message", "Failed to send email");
                        }
                    } else {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "Invalid email or password in database");
                    }
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Server/database error");
        }

        writeJson(response, gson, jsonResponse);
    }

    private void writeJson(HttpServletResponse response, Gson gson, JsonObject jsonResponse)
            throws IOException {
        PrintWriter out = response.getWriter();
        out.print(gson.toJson(jsonResponse));
        out.flush();
    }

    private boolean sendPasswordEmail(String toEmail, String username, String password) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);  // FIXED: Now uses correct port

            Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
                        }
                    });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Gym App - Password Recovery");
            message.setText("Hello " + username + ",\n\n" +
                           "Your account password is: " + password + "\n\n" +
                           "⚠️  For security, change your password after login.\n" +
                           "Do not share this email.\n\n" +
                           "Gym Management Team");

            Transport.send(message);
            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Email failed: " + e.getMessage());  // Better logging
            return false;
        }
    }
}