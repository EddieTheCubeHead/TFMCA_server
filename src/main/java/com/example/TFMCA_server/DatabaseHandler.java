package com.example.TFMCA_server;

import java.sql.*;

//https://www.tutorialspoint.com/jdbc/index.htm
public class DatabaseHandler {
    //Driver name, database URL
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static final String DATABASE_URL = System.getenv("DB_URL");

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

    public static void removeUser (String name) throws SQLException {
        PreparedStatement remove_user = null;

        String remove_string = "DELETE FROM tfmca.users WHERE userName=?";

        remove_user = db_connection.prepareStatement(remove_string);
        remove_user.setString(1, name);
        System.out.println("Deleted user " + name);
        remove_user.executeUpdate();

        remove_user.close();
    }

    public static ResultSet getUser(String username) throws SQLException {
        PreparedStatement check_user = null;

        String check_string = "SELECT * FROM tfmca.users WHERE userName = ?";

        check_user = db_connection.prepareStatement(check_string);
        check_user.setString(1, username);

        return check_user.executeQuery();
    }

    public static void createGame(String user,
                                  String code,
                                  Integer map,
                                  Boolean corporate_era,
                                  Boolean prelude,
                                  Boolean venus,
                                  Boolean colonies,
                                  Boolean turmoil,
                                  Boolean extra_corporations,
                                  Boolean world_government_terraforming,
                                  Boolean must_max_venus,
                                  Boolean turmoil_terraforming_revision) throws SQLException {
        PreparedStatement create_game = null;

        String create_string = "INSERT INTO tfmca.games (player1, gameState, gameCode, gameMap, corporateEra, prelude, venus, colonies, turmoil, extraCorporations, worldGovernmentTerragorming, mustMaxVenus, turmoilTerraformingRevision) VALUES ((SELECT userName FROM tfmca.users WHERE userName=?), 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        create_game = db_connection.prepareStatement(create_string);
        create_game.setString(1, user);
        create_game.setString(2, code);
        create_game.setInt(3, map);
        create_game.setInt(4, corporate_era ? 1 : 0);
        create_game.setInt(5, prelude ? 1 : 0);
        create_game.setInt(6, venus ? 1 : 0);
        create_game.setInt(7, colonies ? 1 : 0);
        create_game.setInt(8, turmoil ? 1 : 0);
        create_game.setInt(9, extra_corporations ? 1 : 0);
        create_game.setInt(10, world_government_terraforming ? 1 : 0);
        create_game.setInt(11, must_max_venus ? 1 : 0);
        create_game.setInt(12, turmoil_terraforming_revision ? 1 : 0);
        create_game.executeUpdate();
        System.out.println("Game created");
    }

    public static ResultSet getGame(String code) throws SQLException {
        PreparedStatement get_game;

        String get_game_string = "SELECT * FROM tfmca.games WHERE code = ?";

        get_game = db_connection.prepareStatement(get_game_string);
        get_game.setString(1, code);
        return get_game.executeQuery();
    }

    public static void addPlayer(String user, String code, String player_position) throws SQLException {
        PreparedStatement add_player = null;

        String add_player_string = String.format("UPDATE tfmca.games SET %s = (SELECT userName FROM tfmca.users WHERE userName=?) WHERE code = ?", player_position);

        add_player = db_connection.prepareStatement(add_player_string);
        add_player.setString(1, user);
        add_player.setString(2, code);
        add_player.executeUpdate();
        add_player.close();
    }
}