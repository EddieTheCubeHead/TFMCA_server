package com.example.TFMCA_server;

import com.example.TFMCA_server.errors.InvalidPasswordException;
import com.example.TFMCA_server.errors.InvalidUsernameException;
import org.jose4j.base64url.Base64;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;

//https://github.com/softgitron/andro-bank-backend/blob/master/server/src/main/java/com/server/controllers/UserController.java
//https://www.baeldung.com/java-password-hashing
//Vahvasti kopioitu, mutta koko serveri-implementaatio kurssin scopen ulkopuolella joten en jaksa välittää, kunhan toimii
public class UserHandler {
    //Jos tehtäisiin tosissaan levitykseen opettelisin tän tekemisen paremmin ja tuunaisin. Tässä tilanteessa ei jaksa.
    // - Eetu

    //Validate the format of the username and the password
    private static Boolean checkUsername(String username) {
        String USERNAME_CHECK_REGEX = "^[A-Za-z0-9_ ]{3,20}$";
        return (username.matches(USERNAME_CHECK_REGEX));
    }

    private static Boolean checkPass(String password) {
        String PASSWORD_CHECK_REGEX = ".{7,}";
        return (password.matches(PASSWORD_CHECK_REGEX));
    }

    public static String create_user(String username, String password_raw) throws InvalidUsernameException, InvalidPasswordException {
        if (!checkUsername(username)) {
            throw new InvalidUsernameException("Username is invalid. Please make sure your username is 3-20 characters long and contains only letters, number spaces and underscores.");
        } else if (!checkPass(password_raw)) {
            throw new InvalidPasswordException("Password is invalid. Please make sure your password is at least seven characters long.");
        }

        String password_hash = hashPassword(password_raw);
        System.out.println("Hashed password: " + password_hash);
        try {
            if (DatabaseHandler.get_user(username).next()) {
                throw new InvalidUsernameException("This username already exists.");
            }
            DatabaseHandler.insertUser(username, password_hash);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return SessionIdHandler.create_session_id(username);
    }

    public static String login_user(String username, String password_raw) throws InvalidUsernameException, InvalidPasswordException {
        try {
            ResultSet user_rs = DatabaseHandler.get_user(username);
            if (!user_rs.next()) {
                System.out.println("User doesn't exist!");
                throw new InvalidUsernameException("Username doesn't exist.");
            }

            String existing_password = user_rs.getString("password");
            String[] existing_password_data = existing_password.split("\\$");
            byte[] salt = Base64.decode(existing_password_data[1]);

            String user_hash = hashPassword(password_raw, salt);

            if (user_hash.equals(existing_password)) {
                return SessionIdHandler.create_session_id(username);
            } else {
                throw new InvalidPasswordException("Incorrect password!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    static String hashPassword(String password_raw, byte[] salt) {
        byte[] hash = null;
        KeySpec spec = new PBEKeySpec(password_raw.toCharArray(), salt, 65536, 128);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            hash = factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println("Error when hashing password.");
        }
        return Base64.encode(hash) + "$" + Base64.encode(salt);
    }

    static String hashPassword(String password_raw) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return hashPassword(password_raw, salt);
    }

}
