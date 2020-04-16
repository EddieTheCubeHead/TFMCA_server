package com.example.TFMCA_server;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
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
            DatabaseHandler.createGame(user, game_code);
            games.put(game_code, new ArrayList<>(Collections.singletonList(session)));
            sendMessage(game_code, null, "testi1");
            return game_code;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Boolean joinGame(String game_code, WebSocketSession session, String user) {
        if (!games.containsKey(game_code)) {
            return false;
        }

        games.get(game_code).add(session);
        String position = String.format("player%d", games.get(game_code).size());
        try {
            System.out.println(user + " " + game_code + " " + position);
            DatabaseHandler.addPlayer(user, game_code, position);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        sendMessage(game_code, null, "testi2");

        return true;
    }

    public static void sendMessage(String game_code, WebSocketSession session, String message) {
        for (WebSocketSession game_session : games.get(game_code)) {
            if (session != null && session == game_session) {
                continue;
            }
            try {
                if (game_session.isOpen()) {
                    System.out.println("WebSocket sending message: " + game_session.getId());
                    game_session.sendMessage(new TextMessage("testi"));
                } else {
                    System.out.println("WebSocket failed to send a message: " + game_session.getId());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
