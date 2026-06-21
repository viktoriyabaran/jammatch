package common.messages;

public final class SessionMessages {

    public record ClientLogin(String nickname, String clientToken) {
    }

    public record SubmitPlaylist(String playlistUrl) {
    }
}
