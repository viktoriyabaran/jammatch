package common.messages;

import java.util.List;

public final class RoomMessages {

    public record RoomConfig(String roomName, int maxPlayers, int rounds, int roundDurationSeconds, boolean hostAudioOnly) {
    }

    public record JoinRoom(String roomCode) {
    }

    public record KickPlayer(int targetUserId) {
    }

    public record CreateRoomResult(String roomCode) {
    }

    public record RoomSettings(int maxPlayers, int rounds, int roundDurationSeconds, boolean hostAudioOnly) {
    }

    public record PlayerInfo(int id, String nickname, boolean hasPlaylist, int songCount) {
    }

    public record LobbyUpdate(String roomCode, String roomName, RoomSettings settings, int hostId,
                              List<PlayerInfo> players) {
    }

    public record RoomClosed(String reason) {
    }
}
