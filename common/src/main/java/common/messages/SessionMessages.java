package common.messages;

import java.util.List;

public final class SessionMessages {

    public record ClientLogin(String nickname, String clientToken) {
    }

    public record SubmitPlaylist(String playlistUrl) {
    }

    public record SavedPlaylist(String url, String name) {
    }

    public record SavedPlaylists(List<SavedPlaylist> items) {
    }
}
