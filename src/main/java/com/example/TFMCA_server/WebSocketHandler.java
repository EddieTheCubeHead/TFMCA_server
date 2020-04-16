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
        String user;
        String password;
        String session_id;
        String game_code;

        switch (identifier) {
            //Eivät vaadi session_id:tä:
            case("new_user"):
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

            case("login"):
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

            //Validin session_id:n vaativat:
            case("create_game"):
                user = contents[1];
                session_id = contents[2];
                if (!SessionIdHandler.verify_session(user, session_id)) {
                    session.sendMessage(new TextMessage("session_exception;Invalid session"));
                } else {
                    game_code = GameSessionHandler.createGame(session, user);
                    session.sendMessage(new TextMessage("game_created;" + game_code));
                }
                break;

            case ("join_game"):
                user = contents[1];
                session_id = contents[2];
                game_code = contents[3];
                if (!SessionIdHandler.verify_session(user, session_id)) {
                    session.sendMessage(new TextMessage("session_exception;Invalid session"));
                } else if (!GameSessionHandler.joinGame(game_code, session, user)){
                    session.sendMessage(new TextMessage("join_exception;Unable to join game"));
                }
                break;

            default:
                System.out.println("Unrecognized message:" + message.getPayload());
        }
    }
}
