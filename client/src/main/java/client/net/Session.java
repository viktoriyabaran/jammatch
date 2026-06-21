package client.net;

import java.util.UUID;
import java.util.prefs.Preferences;

public class Session {

    private final String clientToken = resolveClientToken();
    private int userId;
    private String nickname;

    public int userId() {
        return userId;
    }

    public String nickname() {
        return nickname;
    }

    public String clientToken() {
        return clientToken;
    }

    public void login(int userId, String nickname) {
        this.userId = userId;
        this.nickname = nickname;
    }

    private static String resolveClientToken() {
        String override = System.getenv("JAMMATCH_CLIENT_TOKEN");
        if (override != null && !override.isBlank()) {
            return override;
        }
        Preferences prefs = Preferences.userNodeForPackage(Session.class);
        String token = prefs.get("clientToken", null);
        if (token == null) {
            token = UUID.randomUUID().toString();
            prefs.put("clientToken", token);
        }
        return token;
    }
}
