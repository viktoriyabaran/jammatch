package common.contracts;

public enum CommandType {
    // Login and user management
    CLIENT_LOGIN(100),
    SUBMIT_PLAYLIST(101),
    LIST_SAVED_PLAYLISTS(102),
    VALIDATE_PLAYLIST(103),

    // Room management
    CREATE_ROOM(200),
    JOIN_ROOM(201),
    LEAVE_ROOM(202),
    UPDATE_ROOM_CONFIG(203),
    KICK_PLAYER(204),
    LOBBY_UPDATE(210),
    ROOM_CLOSED(211),

    // Game management
    START_GAME(301),
    SUBMIT_VOTE(302),
    ROUND_START(310),
    ROUND_END(311),
    GAME_OVER(312);

    private final int code;
    CommandType(int code) {
        this.code = code;
    }
    public int code() {
        return code;
    }

    public static CommandType fromCode(int code) {
        for (CommandType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown command: " + code);
    }
}