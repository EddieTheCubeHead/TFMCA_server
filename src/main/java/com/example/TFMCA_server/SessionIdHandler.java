package com.example.TFMCA_server;

import javax.jws.soap.SOAPBinding;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SessionIdHandler {
    private static Map<String, UserIdData> session_id_map = new HashMap<>();
    private static Integer MAX_VERIFICATION_DIFF = 120;

    //https://www.baeldung.com/java-random-string
    public static String create_session_id (String user) {
        String session_id;

        //Tämä pätkä suoraan lähteestä. Koska Serveri on tarkoitettu Proof-of-concept ratkaisuksi, en nähnyt tarpeelliseksi keksiä pyörää uudestaan.
        int left_limit = 48; // numeral '0'
        int right_limit = 122; // letter 'z'
        int target_string_length = 16;
        Random random = new Random();

        //Varmistetaan uniikin id:n luonti. Mahdollisuus duplikaattiin on tosin esimerkiksi tuhannella avaimella suuruusluokkaa 1^-25
        do {
            session_id = random.ints(left_limit, right_limit + 1)
                    .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                    .limit(target_string_length)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
        } while (session_id_map.containsKey(session_id));

        LocalDateTime session_start = LocalDateTime.now();

        session_id_map.put(user, new UserIdData(session_id, session_start));

        System.out.println(user + " logged in with session id " + session_id + " at " + session_start);
        return session_id;
    }

    public static String create_guest_id (String guest) {
        String session_id = create_session_id(guest);
        session_id_map.get(guest).setGuest();
        return session_id;
    }

    public static boolean verifyGuestName (String name) {
        return !session_id_map.containsKey(name);
    }

    /* Sessioiden avainten vanheneminen on toteutettu niin että avaimet vanhenevat MAX_VERIFICATION_DIFF minuutin
     * kuluttua viimeisestä interaktiosta.
     */
    public static Boolean verify_session (String user, String session_id) {

        if (!session_id_map.containsKey(user)) {
            return false;
        }

        UserIdData session_data = session_id_map.get(user);
        String user_session_id = session_data.getSessionId();
        System.out.println(user_session_id);
        LocalDateTime last_verification_time = session_data.getLastVerification();
        Duration since_last_verification = Duration.between(last_verification_time, LocalDateTime.now());
        long duration_minutes = Math.abs(since_last_verification.toMinutes());

        if (user_session_id.equals(session_id) && duration_minutes < MAX_VERIFICATION_DIFF) {
            session_data.setLastVerification();
            return true;
        }
        return false;
    }

    public static void pruneIdMap() {
        for(Map.Entry<String, UserIdData> entry : session_id_map.entrySet()) {
            if (Math.abs(Duration.between(entry.getValue().getLastVerification(), LocalDateTime.now()).toMinutes()) < MAX_VERIFICATION_DIFF) {
                if (entry.getValue().getIsGuest()) {
                    try {
                        DatabaseHandler.removeUser(entry.getKey());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                session_id_map.remove(entry.getKey());
            }
        }
        System.out.println("Session id map pruned.");
    }
}

//Simppeli luokka pitämään sisällään tarvittava data sessiosta.
class UserIdData {
    private Boolean is_guest = false;
    private String session_id;
    private LocalDateTime last_verification;
    public Boolean getIsGuest() {return is_guest;}
    public void setGuest() {is_guest = true;}
    public String getSessionId() {return session_id;}
    public LocalDateTime getLastVerification() {return last_verification;}
    public void setLastVerification() {last_verification = LocalDateTime.now();}
    public UserIdData(String session_id, LocalDateTime session_start) {
        this.session_id = session_id;
        this.last_verification = session_start;
    }
}
