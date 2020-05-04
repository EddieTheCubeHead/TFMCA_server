package com.example.TFMCA_server;

import com.example.TFMCA_server.errors.InvalidPasswordException;
import com.example.TFMCA_server.errors.InvalidUsernameException;
import com.example.TFMCA_server.gameEvents.CardCostPacket;
import com.example.TFMCA_server.gameEvents.CardEventPacket;
import com.example.TFMCA_server.gameEvents.ResourceEventPacket;
import com.example.TFMCA_server.gameEvents.TileEventPacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;


public class WebSocketHandler extends AbstractWebSocketHandler {
    private static GsonBuilder builder = new GsonBuilder();
    private static Gson gson = builder.create();

    static List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String[] contents = message.getPayload().split(Pattern.quote(";"));
        String identifier = contents[0];
        String user;
        String password;
        String session_id;
        String game_code;
        String packet_type;
        String gson_packet;
        Integer action_number;
        Integer generation;
        System.out.println("WebSocket message from session " + session.getId());

        switch (identifier) {
            //Eivät vaadi session_id:tä:
            case "new_user":
                try {
                    user = contents[1];
                    password = contents[2];
                    session_id = UserHandler.create_user(user, password);
                    session.sendMessage(new TextMessage("user_creation_successful;" + session_id));
                } catch (InvalidUsernameException e) {
                    session.sendMessage(new TextMessage("username_exception;" + e.getMessage()));
                } catch (InvalidPasswordException e) {
                    session.sendMessage(new TextMessage("password_exception;" + e.getMessage()));
                }
                break;

            case "login":
                try {
                    user = contents[1];
                    password = contents[2];
                    session_id = UserHandler.login_user(user, password);
                    session.sendMessage(new TextMessage("login_successful;" + session_id));
                } catch (InvalidUsernameException e) {
                    session.sendMessage(new TextMessage("username_exception;" + e.getMessage()));
                } catch (InvalidPasswordException e) {
                    session.sendMessage(new TextMessage("password_exception;" + e.getMessage()));
                }
                break;

            case "logout":
                SessionIdHandler.logoutId(contents[1]);
                session.sendMessage(new TextMessage("logout_successful"));
                break;

            //Validin session_id:n vaativat:
            case "create_game":
                user = contents[1];
                session_id = contents[2];
                if (!SessionIdHandler.verify_session(user, session_id)) {
                    session.sendMessage(new TextMessage("session_exception;Invalid session"));
                } else {
                    game_code = GameSessionHandler.createGame(session, message.getPayload());
                    session.sendMessage(new TextMessage("game_created;" + game_code));
                }
                break;

            case "join_game":
                user = contents[1];
                session_id = contents[2];
                game_code = contents[3];
                if (!SessionIdHandler.verify_session(user, session_id)) {
                    session.sendMessage(new TextMessage("session_exception;Invalid session"));
                } else if (!GameSessionHandler.joinGame(game_code, session, user)){
                    session.sendMessage(new TextMessage("join_exception;Unable to join game"));
                }
                try {
                    session.sendMessage(new TextMessage("game_joined;" + game_code + DatabaseHandler.getGameUsers(game_code)));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;

            case "start_game":
                user = contents[1];
                session_id = contents[2];
                game_code = contents[3];
                if (!SessionIdHandler.verify_session(user, session_id)) {
                    session.sendMessage(new TextMessage("session_exception;Invalid session"));
                }
                GameSessionHandler.sendMessage(game_code, session, "game_start");
                break;

            //Pelin tapahtumat
            case "game_action":
                user = contents[1];
                session_id = contents[2];
                game_code = contents[3];
                packet_type = contents[4];
                gson_packet = contents[5];
                action_number = Integer.parseInt(contents[6]);
                generation = Integer.parseInt(contents[7]);
                if (!SessionIdHandler.verify_session(user, session_id)) {
                    session.sendMessage(new TextMessage("session_exception;Invalid session"));
                }
                switch (packet_type) {
                    case "card_event":
                        CardEventPacket event_packet = gson.fromJson(gson_packet, CardEventPacket.class);
                        try {
                            DatabaseHandler.saveEvent(event_packet, game_code, action_number, generation);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "card_cost":
                        CardCostPacket cost_packet = gson.fromJson(gson_packet, CardCostPacket.class);
                        try {
                            DatabaseHandler.saveEvent(cost_packet, game_code, action_number, generation);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "resource_event":
                        ResourceEventPacket resource_packet = gson.fromJson(gson_packet, ResourceEventPacket.class);
                        try {
                            DatabaseHandler.saveEvent(resource_packet, game_code, action_number, generation);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "tile_event":
                        TileEventPacket tile_packet = gson.fromJson(gson_packet, TileEventPacket.class);
                        try {
                            DatabaseHandler.saveEvent(tile_packet, game_code, action_number, generation);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;

                }
                GameSessionHandler.sendMessage(game_code, session, String.format("game_action;%s;%s", packet_type, gson_packet));
                break;

            default:
                System.out.println("Unrecognized message: " + message.getPayload());
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        session.setBinaryMessageSizeLimit(1024 * 1024);
        session.setTextMessageSizeLimit(1024 * 1024);
    }

    public static void sendToSessions(ArrayList<String> session_id_list, String message) throws IOException {
        for (WebSocketSession session : sessions) {
            if (session_id_list.contains(session.getId())) {
                if (session.isOpen()) {
                    System.out.println("WebSocket sending message to session  " + session.getId());
                    session.sendMessage(new TextMessage(message));
                } else {
                    System.out.println("WebSocket unable to send message to session " + session.getId());
                }
            }
        }
    }

    static void pingAll() throws IOException {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new PingMessage());
            } else {
                System.out.println("Unable to ping session " + session.getId());
                sessions.remove(session);
            }
        }
    }

    public static void initHeartbeat() {
        SessionHeartbeat session_heartbeat = new SessionHeartbeat();
        Timer session_heartbeat_timer = new Timer();

        session_heartbeat_timer.schedule(session_heartbeat, 100, 10000);
    }
}

class SessionHeartbeat extends TimerTask {
    public void run() {
        try {
            WebSocketHandler.pingAll();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
