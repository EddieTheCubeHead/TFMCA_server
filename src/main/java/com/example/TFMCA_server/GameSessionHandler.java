package com.example.TFMCA_server;

import com.example.TFMCA_server.DatabaseHandler;
import com.example.TFMCA_server.WebSocketHandler;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class GameSessionHandler {
    private static HashMap<String, ArrayList<String>> games = new HashMap<>();

    public static String createGame(WebSocketSession session, String message) {

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

        System.out.println("Game code created succesfully: " + game_code);

        try {
            String [] game_data = message.split(Pattern.quote(";"));
            DatabaseHandler.createGame(game_data[1], game_code);
            games.put(game_code, new ArrayList<>(Collections.singletonList(session.getId())));

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

        ArrayList<String> game_sessions = games.get(game_code);
        game_sessions.add(session.getId());
        games.put(game_code, game_sessions);
        for (String session_debug : game_sessions) {
            System.out.println(session_debug);
        }
        String position = String.format("player%d", games.get(game_code).size());
        try {
            System.out.println(user + " " + game_code + " " + position);
            DatabaseHandler.addPlayer(user, game_code, position);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        sendMessage(game_code, session, "player_joined;" + user);
        return true;
    }

    public static Boolean checkCode(String game_code) {
        return games.containsKey(game_code);
    }

    public static void sendMessage(String game_code, WebSocketSession session, String message) {
        if (!games.containsKey(game_code)) {
            return;
        }

        ArrayList<String> sessions_to_send = new ArrayList<>(games.get(game_code));

        for (String session_debug : sessions_to_send) {
            System.out.println(session_debug);
        }

        if (session != null) {
            sessions_to_send.remove(session.getId());
        }

        System.out.println("Sending messages to sessions in game " + game_code);

        try {
            WebSocketHandler.sendToSessions(sessions_to_send, message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}