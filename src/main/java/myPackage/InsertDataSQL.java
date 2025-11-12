package myPackage;
import java.sql.*;
import java.text.SimpleDateFormat;
public class InsertDataSQL {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String url = "jdbc:mysql://localhost:3306/gym"; // your DB name
        String user = "root"; // change if needed
        String pass = "Ashish_mca@1234"; // change if needed

        
        
        // ----***Member Table***----
       /* String[] names = {"Amit Sharma", "Priya Singh", "Rahul Mehta", "Sneha Gupta", "Vikas Yadav",
                          "Riya Patel", "Ankit Verma", "Neha Reddy", "Saurabh Jain", "Pooja Nair"};
        String[] gender = {"Male", "Female", "Male", "Female", "Male", "Female", "Male", "Female", "Male", "Female"};
        String[] dobList = {"12-05-1995", "25-03-1998", "17-11-1994", "08-01-2000", "02-06-1996",
                            "15-07-1999", "09-12-1997", "10-09-1998", "28-04-1995", "30-08-2001"};
        String[] phone = {"9876543210", "8765432109", "9988776655", "9090909090", "9123456789",
                          "9345678901", "9456789012", "9567890123", "9678901234", "9789012345"};
        String[] address = {"Delhi", "Mumbai", "Kolkata", "Bangalore", "Lucknow",
                            "Pune", "Hyderabad", "Chennai", "Jaipur", "Ahmedabad"};

        try {
            Connection con = DriverManager.getConnection(url, user, pass);
            String sql = "INSERT INTO member (Member_ID, Name, DOB, Gender, Phone_no, Address) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            for (int i = 0; i < 10; i++) {
                int id = 500 + i; // Member IDs (must match foreign key later)
                java.util.Date utilDate = sdf.parse(dobList[i]);
                java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());

                ps.setInt(1, id);
                ps.setString(2, names[i]);
                ps.setDate(3, sqlDate);
                ps.setString(4, gender[i]);
                ps.setString(5, phone[i]);
                ps.setString(6, address[i]);

                ps.addBatch();
            }

            ps.executeBatch();
            System.out.println("✅ 10 member records inserted successfully!");

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        
        
        
        /*
                // ----***Membership Table***----
                String[] types = {"General", "PT"};

                try {
                    Connection con = DriverManager.getConnection(url, user, pass);
                    String sql = "INSERT INTO membership (Membership_ID, Start_date, End_date, Membership_type, Member_ID, Total_fee) VALUES (?, ?, ?, ?, ?, ?)";
                    PreparedStatement ps = con.prepareStatement(sql);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

                    for (int i = 0; i < 10; i++) {
                        int membershipId = 2000 + i;        // Unique Membership IDs
                        int memberId = 500 + i;             // Must exist in member table
                        String type = types[i % 2];         // Alternate between General / PT

                        // Example start/end dates
                        String start = "01-0" + ((i % 9) + 1) + "-2025";
                        String end = "30-0" + ((i % 9) + 1) + "-2025";

                        // Convert dd-MM-yyyy → java.sql.Date
                        java.util.Date startUtil = sdf.parse(start);
                        java.util.Date endUtil = sdf.parse(end);
                        java.sql.Date startDate = new java.sql.Date(startUtil.getTime());
                        java.sql.Date endDate = new java.sql.Date(endUtil.getTime());

                        // Total fee logic
                        int totalFee = (type.equals("PT")) ? 4000 : 2000;

                        // Set parameters
                        ps.setInt(1, membershipId);
                        ps.setDate(2, startDate);
                        ps.setDate(3, endDate);
                        ps.setString(4, type);
                        ps.setInt(5, memberId);
                        ps.setInt(6, totalFee);

                        ps.addBatch();
                    }

                    ps.executeBatch();
                    System.out.println("✅ 10 membership records inserted successfully!");

                    con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
        
        
        
        
        
        // ----***Payment Table***----
        try {
            Connection con = DriverManager.getConnection(url, user, pass);
            String sql = "INSERT INTO payment (Payment_ID, Pay_date, Paid_fee, Pending_fee, Due_date, Membership_ID) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            for (int i = 0; i < 10; i++) {
                int paymentId = 3000 + i;          // Unique Payment IDs
                int membershipId = 2000 + i;       // Must exist in membership table

                // Example payment and due dates
                String payDateStr = "05-0" + ((i % 9) + 1) + "-2025";
                String dueDateStr = "15-0" + ((i % 9) + 1) + "-2025";

                // Convert to java.sql.Date
                java.util.Date payUtil = sdf.parse(payDateStr);
                java.util.Date dueUtil = sdf.parse(dueDateStr);
                java.sql.Date payDate = new java.sql.Date(payUtil.getTime());
                java.sql.Date dueDate = new java.sql.Date(dueUtil.getTime());

                // Example paid and pending fee logic
                int paidFee = (i % 2 == 0) ? 2000 : 4000;
                int pendingFee = (paidFee == 2000) ? 2000 : 0;

                // Set parameters
                ps.setInt(1, paymentId);
                ps.setDate(2, payDate);
                ps.setInt(3, paidFee);
                ps.setInt(4, pendingFee);
                ps.setDate(5, dueDate);
                ps.setInt(6, membershipId);

                ps.addBatch();
            }

            ps.executeBatch();
            System.out.println("✅ 10 payment records inserted successfully!");

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
