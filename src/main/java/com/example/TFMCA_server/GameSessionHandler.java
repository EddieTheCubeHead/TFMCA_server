package com.example.TFMCA_server;

import org.springframework.web.socket.WebSocketSession;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class GameSessionHandler {
    private static HashMap<String, ArrayList<WebSocketSession>> games = new HashMap<>();

    public static String createGame(WebSocketSession session, String user) {

        //TODO tämä funktioon, koska käytetään kahdessa paikassa
        String game_code;
        //Tämä pätkä suoraan lähteestä. Koska Serveri on tarkoitettu Proof-of-concept ratkaisuksi, en nähnyt tarpeelliseksi keksiä pyörää uudestaan.
        int left_limit = 48; // numeral '0'
        int right_limit = 122; // letter 'z'
        int target_string_length = 6;
        Random random = new Random();

        //Varmistetaan uniikin koodin luonti.
        do {
            game_code = random.ints(left_limit, right_limit + 1)
                    .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                    .limit(target_string_length)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
        } while (games.containsKey(game_code));

        try {
            System.out.println(user);
            DatabaseHandler.createGame(user, game_code);
            games.put(game_code, new ArrayList<WebSocketSession>(Collections.singletonList(session)));
            return game_code;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void joinGame(String game_id, WebSocketSession session, String user) {

    }

    public static void sendMessage(String game_id, String message, String user) {

    }
}

class GameData {
    private ArrayList<WebSocketSession> sessions;
    private Integer game_id;

    public ArrayList<WebSocketSession> getSessions() {return sessions;}
    public Integer getGameId() {return game_id;}

    public void addSession(WebSocketSession session) {
        sessions.add(session);
    }

    public GameData(Integer game_id) {
        this.game_id = game_id;
    }
}
