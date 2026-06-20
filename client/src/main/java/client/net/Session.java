package client.net;

public class Session {

    private int userId;
    private String nickname;

    public int userId() {
        return userId;
    }

    public String nickname() {
        return nickname;
    }

    public void login(int userId, String nickname) {
        this.userId = userId;
        this.nickname = nickname;
    }
}