package com.example.TFMCA_server;

import java.sql.*;

//https://www.tutorialspoint.com/jdbc/index.htm
public class DatabaseHandler {
    //Driver name, database URL
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static final String DATABASE_URL = "jdbc:mysql://localhost/tfmca";

    private static final String DB_USER = System.getenv("TFMCA_DB_USER");
    private static final String DB_PASSWORD = System.getenv("TFMCA_DB_PASSWORD");

    private static Connection db_connection = null;

    public static void initialize() {
        try {
            System.out.println("Getting a database connection.");
            db_connection = DriverManager.getConnection(DATABASE_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connection established.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertUser (String username, String password_hash) throws SQLException {
        PreparedStatement insert_user = null;

        String insert_string = "INSERT INTO tfmca.users (userName, password) VALUES (?, ?)";

        insert_user = db_connection.prepareStatement(insert_string);
        insert_user.setString(1, username);
        insert_user.setString(2, password_hash);
        System.out.println("Creating user " + username);
        insert_user.executeUpdate();

        insert_user.close();
    }

    public static void insertGuest (String name) throws SQLException {
        PreparedStatement insert_guest = null;

        String insert_string = "INSERT INTO tfmca.users (userName, isGuest) VALUES (?, 1)";

        insert_guest = db_connection.prepareStatement(insert_string);
        insert_guest.setString(1, name);
        System.out.println("Creating guest " + name);
        insert_guest.executeUpdate();

        insert_guest.close();
    }

    public static void removeUser (String name) throws SQLException {
        PreparedStatement remove_user = null;

        String remove_string = "DELETE FROM tfmca.users WHERE userName=?";

        remove_user = db_connection.prepareStatement(remove_string);
        remove_user.setString(1, name);
        System.out.println("Deleted user " + name);
        remove_user.executeUpdate();

        remove_user.close();
    }

    public static ResultSet get_user(String username) throws SQLException {
        PreparedStatement check_user = null;

        String check_string = "SELECT * FROM tfmca.users WHERE userName = ?";

        check_user = db_connection.prepareStatement(check_string);
        check_user.setString(1, username);

        return check_user.executeQuery();
    }

    public static void pruneGuests() throws SQLException {
        PreparedStatement remove_guests = null;

        String remove_string = "DELETE FROM tfmca.users WHERE isGuest=1";

        remove_guests = db_connection.prepareStatement(remove_string);
        System.out.println("Deleted guest users.");
        remove_guests.executeUpdate();

        remove_guests.close();
    }

    public static void createGame(String user, String code) throws SQLException {
        PreparedStatement create_game = null;

        String create_string = "INSERT INTO tfmca.games (player1, state, code) VALUES ((SELECT userName FROM tfmca.users WHERE userName=?), 'setup', ?)";

        create_game = db_connection.prepareStatement(create_string);
        create_game.setString(1, user);
        create_game.setString(2, code);
        System.out.println("Game created");
        create_game.executeUpdate();
    }
}