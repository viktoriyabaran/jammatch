package common.contracts;

public enum ResponseCode {
    OK(200),

    // Login and user management
    INVALID_PLAYLIST(400),
    LOGIN_FAILED(401),

    // Room management
    NOT_HOST(403),
    ROOM_NOT_FOUND(404),
    CANNOT_KICK_HOST(405),
    ROOM_FULL(409),
    NICKNAME_TAKEN(422),

    // Game management
    GAME_ALREADY_STARTED(423),

    INTERNAL_ERROR(500);

    private final int code;
    ResponseCode(int code) { this.code = code; }
    public int code() { return code; }

    public static ResponseCode fromCode(int code) {
        for (ResponseCode r : values()) {
            if (r.code == code) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown response code: " + code);
    }
}
