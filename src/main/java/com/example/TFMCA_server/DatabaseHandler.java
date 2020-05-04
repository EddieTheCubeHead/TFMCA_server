package com.example.TFMCA_server;

import com.example.TFMCA_server.gameEvents.CardCostPacket;
import com.example.TFMCA_server.gameEvents.CardEventPacket;
import com.example.TFMCA_server.gameEvents.ResourceEventPacket;
import com.example.TFMCA_server.gameEvents.TileEventPacket;

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
                                  String code) throws SQLException {
        PreparedStatement create_game = null;

        String create_string = "INSERT INTO tfmca.games (player1, gameState, gameCode) VALUES ((SELECT userName FROM tfmca.users WHERE userName=?), 0, ?)";

        create_game = db_connection.prepareStatement(create_string);
        create_game.setString(1, user);
        create_game.setString(2, code);
        create_game.executeUpdate();
        create_game.close();
        System.out.println("Game created");
    }

    public static Integer getGameId(String code) throws SQLException {
        PreparedStatement get_game;

        String get_game_string = "SELECT * FROM tfmca.games WHERE gameCode = ?";

        get_game = db_connection.prepareStatement(get_game_string);
        get_game.setString(1, code);
        ResultSet rs = get_game.executeQuery();
        if (!rs.next()) {
            return null;
        }
        return rs.getInt("id");
    }

    public static String getGameUsers(String code) throws SQLException {
        PreparedStatement get_game;

        String get_game_string = "SELECT * FROM tfmca.games WHERE gameCode = ?";

        get_game = db_connection.prepareStatement(get_game_string);
        get_game.setString(1, code);
        ResultSet rs = get_game.executeQuery();
        if (!rs.next()) {
            rs.close();
            return null;
        }
        StringBuilder names = new StringBuilder();
        for (int i = 1; i < 6 ; i++) {
            names.append(";");
            if (rs.getString(String.format("player%d", i)) == null) {
                continue;
            }
            names.append(rs.getString(String.format("player%d", i)));
        }
        rs.close();
        return names.toString();
    }

    public static void addPlayer(String user, String code, String player_position) throws SQLException {
        PreparedStatement add_player = null;

        String add_player_string = String.format("UPDATE tfmca.games SET %s = (SELECT userName FROM tfmca.users WHERE userName=?) WHERE gameCode = ?", player_position);

        add_player = db_connection.prepareStatement(add_player_string);
        add_player.setString(1, user);
        add_player.setString(2, code);
        add_player.executeUpdate();
        add_player.close();
    }

    public static void saveEvent(CardCostPacket packet, String game_code, Integer action_number, Integer generation) throws SQLException {
        PreparedStatement save_event;

        String save_string = "INSERT INTO tfmca.cardcostevents (parentGame, player, money, steel, titanium, heat, plantResource, floaterResource, actionNumber, generation) VALUES ((SELECT id FROM tfmca.games WHERE id=?), (SELECT userName FROM tfmca.users WHERE userName=?), ?, ?, ?, ?, ?, ?, ?, ?)";

        Integer game_id = getGameId(game_code);
        if (game_id == null) {
            System.out.println("Error recording card cost event! Game id not found.");
            return;
        }

        save_event = db_connection.prepareStatement(save_string);
        save_event.setInt(1, game_id);
        save_event.setString(2, packet.player_name);
        save_event.setInt(3, packet.money);
        save_event.setInt(4, packet.steel);
        save_event.setInt(5, packet.titanium);
        save_event.setInt(6, packet.heat);
        save_event.setInt(7, packet.plant_resources);
        save_event.setInt(8, packet.floater_resources);
        save_event.setInt(9, action_number);
        save_event.setInt(10, generation);

        save_event.executeUpdate();
        save_event.close();
    }

    public static void saveEvent(CardEventPacket packet, String game_code, Integer action_number, Integer generation) throws SQLException {
        PreparedStatement save_event;

        String save_string = "INSERT INTO tfmca.cardplayevents (parentGame, player, card, metadata, actionNumber, generation) VALUES ((SELECT id FROM tfmca.games WHERE id=?), (SELECT userName FROM tfmca.users WHERE userName=?), ?, ?, ?, ?)";

        Integer game_id = getGameId(game_code);
        if (game_id == null) {
            System.out.println("Error recording card play event! Game id not found.");
            return;
        }

        save_event = db_connection.prepareStatement(save_string);
        save_event.setInt(1, game_id);
        save_event.setString(2, packet.player_name);
        save_event.setString(3, packet.card_name);
        save_event.setInt(4, packet.metadata);
        save_event.setInt(5, action_number);
        save_event.setInt(6, generation);

        save_event.executeUpdate();
        save_event.close();
    }

    public static void saveEvent(ResourceEventPacket packet, String game_code, Integer action_number, Integer generation) throws SQLException {
        PreparedStatement save_event;

        String save_string = "INSERT INTO tfmca.cardplayevents (parentGame, card, changeAmount, actionNumber, generation) VALUES ((SELECT id FROM tfmca.games WHERE id=?), ?, ?, ?, ?)";

        Integer game_id = getGameId(game_code);
        if (game_id == null) {
            System.out.println("Error recording resource event! Game id not found.");
            return;
        }

        save_event = db_connection.prepareStatement(save_string);
        save_event.setInt(1, game_id);
        save_event.setString(2, packet.card_name);
        save_event.setInt(3, packet.change);
        save_event.setInt(4, action_number);
        save_event.setInt(5, generation);

        save_event.executeUpdate();
        save_event.close();
    }

    public static void saveEvent(TileEventPacket packet, String game_code, Integer action_number, Integer generation) throws SQLException {
        PreparedStatement save_event;

        String save_string = "INSERT INTO tfmca.cardplayevents (parentGame, player, tileType, xCoord, yCoord, actionNumber, generation) VALUES ((SELECT id FROM tfmca.games WHERE id=?), (SELECT userName FROM tfmca.users WHERE userName=?), ?, ?, ?, ?, ?)";

        Integer game_id = getGameId(game_code);
        if (game_id == null) {
            System.out.println("Error recording tile event! Game id not found.");
            return;
        }

        save_event = db_connection.prepareStatement(save_string);
        save_event.setInt(1, game_id);
        save_event.setString(2, packet.player_name);
        save_event.setString(3, packet.tile_type.name());
        save_event.setInt(4, packet.x_coord);
        save_event.setInt(5, packet.y_coord);
        save_event.setInt(6, action_number);
        save_event.setInt(7, generation);

        save_event.executeUpdate();
        save_event.close();
    }
}