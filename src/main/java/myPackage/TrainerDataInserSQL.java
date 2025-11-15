package myPackage;
import java.sql.*;
import java.lang.ClassNotFoundException;


public class TrainerDataInserSQL {

    public static void main(String[] args)throws ClassNotFoundException {

        String url = "jdbc:mysql://localhost:3306/gym";
        String user = "root";
        String pass = "stud102024su";

        String[] names = {"Rajesh Kumar","Amit Sharma", "Suresh Patel", "Vikram Singh", "Anjali Verma", "Priya Nair", "Karan Desai", "Rohit Reddy", "Deepa Mehta", "Manoj Kumar"};
        String[] gender = {"Male","Male","Male","Male", "Female","Female", "Male", "Male","Female","Male"};
        String[] phone = {"9876501234", "9123405678", "9988776655", "8765432109", "7890123456", "9654321870", "9345678021", "8012345678", "7567890123", "6234509876"};
        String[] address = {
            "12A Rajendra Nagar, New Delhi, Delhi, 110060",
            "45 Sector 22, Noida, Uttar Pradesh, 201301",
            "Plot 7, Satellite Road, Ahmedabad, Gujarat, 380015",
            "House No. 6, Model Town, Lucknow, Uttar Pradesh, 226001",
            "3B, Lajpat Nagar II, New Delhi, Delhi, 110024",
            "18/4 MG Road, Ernakulam, Kerala, 682016",
            "22 Shastri Nagar, Jaipur, Rajasthan, 302016",
            "Flat 202, Banjara Hills, Hyderabad, Telangana, 500",
            "9 Park Street, Kolkata, West Bengal, 700016",
            "56/2B, Jayanagar 4th T, Bengaluru, Karnataka, 560041"
        };
        String[] id = {"101", "102", "103", "104","105", "106", "107","108", "109", "110"};
        Class.forName("com.mysql.cj.jdbc.Driver");

        try {
            Connection con = DriverManager.getConnection(url, user, pass);
            String sql = "INSERT INTO trainer(Trainer_ID, Name, Gender, Phone_no, Address) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);

            for (int i = 0; i < id.length; i++) {
                ps.setString(1, id[i]);
                ps.setString(2, names[i]);
                ps.setString(3, gender[i]);
                ps.setString(4, phone[i]);
                ps.setString(5, address[i]);

                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("✅ 10 member records inserted successfully!");

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
