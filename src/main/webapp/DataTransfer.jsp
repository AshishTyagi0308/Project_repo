<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Member Details</title>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <style>
        table, th, td {
            border: 1px solid black;
            border-collapse: collapse;
            padding: 8px;
        }
    </style>
</head>
<body>
    <h2>Member Details</h2>
    <button id="loadData">Load Member Data</button>
    <div id="result"></div>

    <script>
        $(document).ready(function() {
            $('#loadData').click(function() {
                $.ajax({
                    url: 'DataTransmission', // Adjust URL if needed
                    type: 'GET',
                    dataType: 'json',
                    success: function(data) {
                        let table = '<table><tr><th>Member ID</th><th>Name</th><th>DOB</th><th>Gender</th><th>Phone No</th><th>Address</th></tr>';
                        $.each(data, function(index, member) {
                            table += '<tr>';
                            table += '<td>' + member.Member_ID + '</td>';
                            table += '<td>' + member.Name + '</td>';
                            table += '<td>' + member.DOB + '</td>';
                            table += '<td>' + member.Gender + '</td>';
                            table += '<td>' + member.Phone_no + '</td>';
                            table += '<td>' + member.Address + '</td>';
                            table += '</tr>';
                        });
                        table += '</table>';
                        $('#result').html(table);
                    },
                    error: function(xhr, status, error) {
                        $('#result').html('<p>Error: ' + error + '</p>');
                    }
                });
            });
        });
    </script>
</body>
</html>
