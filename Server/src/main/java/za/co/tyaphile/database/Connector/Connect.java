package za.co.tyaphile.database.Connector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Connect {
    private static Connection connection;
    static private PreparedStatement preparedStatement;

    public static boolean createDatabase(final String database, final String user, final String password) {
        String sql = "CREATE DATABASE IF NOT EXISTS " + database;
        boolean create = false;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost/", user, password);
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.executeUpdate();
            create = true;
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error: " + e);
            for(StackTraceElement ste: e.getStackTrace()) {
                System.err.println("Error: " + ste);
            }
        }
        return create;
    }

    public static Connection getConnection(final String database) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error: " + e);
            for(StackTraceElement ste: e.getStackTrace()) {
                System.err.println("Error: " + ste);
            }
        }
        return connection;
    }

    public static Connection getConnection(final String database, final String user, final String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost/" + database, user, password);
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error: " + e);
            for(StackTraceElement ste: e.getStackTrace()) {
                System.err.println("Error: " + ste);
            }
        }
        return connection;
    }
}