package myPackage;

import java.nio.file.*;
import java.sql.*;
import java.util.Base64;

public class TestPhotoFromDB {
    private static final String URL  = "jdbc:mysql://localhost:3306/gym";
    private static final String USER = "root";
    private static final String PASS = "Ashish_mca@1234";

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
            int memberId = 536; // test member id

            String sql = "SELECT Photo FROM member WHERE Member_ID = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, memberId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String photoString = rs.getString("Photo");
                if (photoString == null || photoString.isEmpty()) {
                    System.out.println("No photo string found for this member.");
                    return;
                }

                // If it has data URL prefix, strip it:
                int commaIndex = photoString.indexOf(',');
                String base64Part = commaIndex >= 0
                        ? photoString.substring(commaIndex + 1)
                        : photoString;

                byte[] imageBytes = Base64.getDecoder().decode(base64Part);

                Path out = Paths.get("test-photo-from-db.jpg");
                Files.write(out, imageBytes);
                System.out.println("Written file: " + out.toAbsolutePath());
            } else {
                System.out.println("Member not found.");
            }
        }
    }
}