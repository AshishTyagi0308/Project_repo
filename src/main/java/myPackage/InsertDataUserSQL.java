package myPackage;
import java.sql.*;

public class InsertDataUserSQL {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String url = "jdbc:mysql://localhost:3306/gym"; // your DB name
        String user = "root"; // change if needed
        String pass = "stud102024su";
        
        String[] names= {"Ashish", "Gopal", "Krishna ", "Yash Tyagi"};
        String[] password= {"abcd@1234", "123", "mno@456", "uvw@789"};
        String[] role= {"admin", "admin", "admin", "therapist"};
        String[] email= {"ashish.tyagi@gmail.com", "gopalnavetia@gmail.com", "krishna.yadav@gmail.com", "yash.yt@gmail.com"};
        
        try {
            Connection con = DriverManager.getConnection(url, user, pass);
            String sql = "INSERT INTO user (Username, Password, Role, Email) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            
            for(int i=0;i<4;i++) {
            ps.setString(1, names[i]);
            ps.setString(2, password[i]);
            ps.setString(3, role[i]);
            ps.setString(4, email[i]);
            
            ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("✅ 4 records inserted successfully!");

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

}
