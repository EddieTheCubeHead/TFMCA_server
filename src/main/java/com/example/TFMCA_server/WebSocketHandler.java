package com.example.TFMCA_server;

import com.example.TFMCA_server.errors.InvalidPasswordException;
import com.example.TFMCA_server.errors.InvalidUsernameException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.regex.Pattern;

public class WebSocketHandler extends AbstractWebSocketHandler {
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String[] contents = message.getPayload().split(Pattern.quote(";"));
        String identifier = contents[0];

        switch (identifier) {
            //Eivät vaadi session_id:tä:
            case("new_user"):
                try {
                    String user = contents[1];
                    String password = contents[2];
                    String session_id = UserHandler.create_user(user, password);
                    session.sendMessage(new TextMessage("user_creation_successful;" + session_id));
                } catch (InvalidUsernameException e) {
                    session.sendMessage(new TextMessage("username_exception;" + e.getMessage()));
                } catch (InvalidPasswordException e) {
                    session.sendMessage(new TextMessage("password_exception;" + e.getMessage()));
                }
                break;

            case("login"):
                try {
                    String user = contents[1];
                    String password = contents[2];
                    String session_id = UserHandler.login_user(user, password);
                    session.sendMessage(new TextMessage("login_successful;" + session_id));
                } catch (InvalidUsernameException e) {
                    session.sendMessage(new TextMessage("username_exception;" + e.getMessage()));
                } catch (InvalidPasswordException e) {
                    session.sendMessage(new TextMessage("password_exception;" + e.getMessage()));
                }
                break;

            case("guest_login"):
                try {
                    String guest = contents[1];

                    if (DatabaseHandler.get_user(guest).next()) {
                        session.sendMessage(new TextMessage("username_exception;Invalid guest name"));
                        return;
                    }
                    DatabaseHandler.insertGuest(guest);
                    String session_id = SessionIdHandler.create_guest_id(guest);
                    session.sendMessage(new TextMessage("guest_login_successful;" + session_id));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            case("quest_join"):
                //TODO vieraana liittyminen peliin
                break;

            //Validin session_id:n vaativat:
            case("create_game"):
                System.out.println(message.getPayload());
                String user = contents[1];
                String session_id = contents[2];
                if (!SessionIdHandler.verify_session(user, session_id)) {
                    session.sendMessage(new TextMessage("session_exception;Invalid session"));
                }
                String game_code = GameSessionHandler.createGame(session, user);
                session.sendMessage(new TextMessage("game_created;" + game_code));
                break;

            default:
                System.out.println("Unrecognized message:" + message.getPayload());
        }
    }
}
