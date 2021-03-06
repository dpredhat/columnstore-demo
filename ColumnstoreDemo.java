import java.sql.*;

public class ColumnstoreDemo {

    public static void main(String[] args) {
        //Change 
        String connectionUrl = "jdbc:sqlserver://localhost:1433;databaseName=master;user=sa;password=Yukon900";

        // Load SQL Server JDBC driver and establish connection.
        try {
            // Load SQL Server JDBC driver and establish connection.
            System.out.print("Connecting to SQL Server ... ");
            
            try (Connection connection = DriverManager.getConnection(connectionUrl)) {
                System.out.println("Connected!");

                //Get and show the version information of the server
                String sql = "SELECT @@VERSION";
                try (Statement statement = connection.createStatement();
                     ResultSet resultSet = statement.executeQuery(sql)) {
                    while (resultSet.next()) {
                        String version = resultSet.getString(1);
                        System.out.println("Version: " + version);
                    }
                }

                // Create a sample database
                System.out.print("Dropping and creating database 'Example' ... ");
                sql = "DROP DATABASE IF EXISTS [Example]; CREATE DATABASE [Example]";
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(sql);
                    System.out.println("Created!");
                }

                // Insert 5 million rows into the table 'Orders'
                System.out.print("Inserting 5 million rows into table 'Orders'. This takes ~15 seconds, please wait ... ");
                sql = new StringBuilder()
                        .append("USE Example; ")
                        .append("WITH a AS (SELECT * FROM (VALUES(1),(2),(3),(4),(5),(6),(7),(8),(9),(10)) AS a(a)) ")
                        .append("SELECT TOP(5000000) ")
                        .append("ROW_NUMBER() OVER (ORDER BY a.a) AS OrderItemId ")
                        .append(",a.a + b.a + c.a + d.a + e.a + f.a + g.a + h.a AS OrderId ")
                        .append(",a.a * 10 AS Price ")
                        .append(",CONCAT(a.a, N' ', b.a, N' ', c.a, N' ', d.a, N' ', e.a, N' ', f.a, N' ', g.a, N' ', h.a) AS ProductName ")
                        .append("INTO Orders ")
                        .append("FROM a, a AS b, a AS c, a AS d, a AS e, a AS f, a AS g, a AS h; ")
                        .toString();
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(sql);
                    System.out.println("Done!");
                }

                // Execute SQL query without a columnstore index
                long elapsedTimeWithoutIndex = SumPrice(connection);
                System.out.println("Query time WITHOUT columnstore index (ms): " + elapsedTimeWithoutIndex);
                
                //Add a columnstore index
                System.out.print("Adding a columnstore index to the 'Orders' table.  Please wait ... ");
                sql = "CREATE CLUSTERED COLUMNSTORE INDEX columnstoreindex ON Orders;";
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(sql);
                    System.out.println("Done!");
                }

                // Execute the same SQL query again after the columnstore index is added
                long elapsedTimeWithIndex = SumPrice(connection);
                System.out.println("Query time WITH columnstore index (ms): " + elapsedTimeWithIndex);

                //Show results
                System.out.println("Performance improvement with columnstore index: " + elapsedTimeWithoutIndex/elapsedTimeWithIndex + "x!");
            }
        } catch (Exception e) {
            System.out.println("");
            e.printStackTrace();
        }
    }

    public static long SumPrice (Connection connection){
        String sql = "SELECT SUM(Price) FROM Orders;";
        long startTime = System.currentTimeMillis();
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                return elapsedTime;
            }
        } catch (Exception e) {
            System.out.println("");
            e.printStackTrace();
        }
        return 0;
    }
}
