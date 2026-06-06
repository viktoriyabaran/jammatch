package common.messages;

public final class SessionMessages {

    public record ClientLogin(String nickname) {
    }

    public record SubmitPlaylist(String playlistUrl) {
    }
}
