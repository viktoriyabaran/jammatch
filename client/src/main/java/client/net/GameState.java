package client.net;

import common.messages.GameMessages.RoundEnd;
import common.messages.GameMessages.RoundResult;
import common.messages.GameMessages.RoundStart;
import common.messages.RoomMessages.LobbyUpdate;
import common.messages.RoomMessages.PlayerInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameState {

    private final int totalRounds;
    private final int hostId;
    private final boolean hostAudioOnly;
    private final List<Integer> order = new ArrayList<>();
    private final Map<Integer, String> nicknames = new HashMap<>();
    private final Map<Integer, Integer> badges = new HashMap<>();
    private final Map<Integer, Integer> scores = new HashMap<>();
    private final Map<Integer, Integer> correct = new HashMap<>();

    public GameState(LobbyUpdate lobby) {
        this.totalRounds = lobby.settings().rounds();
        this.hostId = lobby.hostId();
        this.hostAudioOnly = lobby.settings().hostAudioOnly();
        List<PlayerInfo> players = lobby.players();
        for (int i = 0; i < players.size(); i++) {
            PlayerInfo p = players.get(i);
            order.add(p.id());
            nicknames.put(p.id(), p.nickname());
            badges.put(p.id(), i % 7 + 1);
            scores.put(p.id(), 0);
            correct.put(p.id(), 0);
        }
    }

    public int totalRounds() {
        return totalRounds;
    }

    public int hostId() {
        return hostId;
    }

    public boolean hostAudioOnly() {
        return hostAudioOnly;
    }

    public List<Integer> order() {
        return order;
    }

    public String nickname(int userId) {
        return nicknames.getOrDefault(userId, "player");
    }

    public int badge(int userId) {
        return badges.getOrDefault(userId, 1);
    }

    public int score(int userId) {
        return scores.getOrDefault(userId, 0);
    }

    public int correct(int userId) {
        return correct.getOrDefault(userId, 0);
    }

    public void applyRoundStart(RoundStart rs) {
        if (rs.roundNumber() == 1) {
            for (Integer id : order) {
                scores.put(id, 0);
                correct.put(id, 0);
            }
        }
    }

    public void applyRoundEnd(RoundEnd re) {
        for (RoundResult r : re.results()) {
            scores.put(r.userId(), r.totalScore());
            if (r.correct()) {
                correct.merge(r.userId(), 1, Integer::sum);
            }
        }
    }
}
