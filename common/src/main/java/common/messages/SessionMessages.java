package common.messages;

import java.util.List;

public final class SessionMessages {

    public record ClientLogin(String nickname, String clientToken) {
    }

    public record LookupNickname(String clientToken) {
    }

    public record SubmitPlaylist(String playlistUrl) {
    }

    public record ValidatePlaylist(String playlistUrl) {
    }

    public record SavedPlaylist(String url, String name, int trackCount, String coverUrl) {
    }

    public record SavedPlaylists(List<SavedPlaylist> items) {
    }
}
