package common.messages;

import java.util.List;

public final class GameMessages {

    public record SubmitVote(int roundNumber, int votedUserId) {
    }

    public record RoundStart(int roundNumber, int totalRounds, String videoId, int durationSeconds, List<Integer> options, String coverUrl) {
    }

    public record RoundResult(int userId, int votedUserId, boolean correct, int roundPoints, int totalScore) {
    }

    public record RoundEnd(int roundNumber, int correctUserId, List<RoundResult> results, String videoId, String title, String artist, String coverUrl) {
    }

    public record LeaderboardEntry(int userId, String nickname, int score, int rank) {
    }

    public record GameOver(List<LeaderboardEntry> leaderboard) {
    }

    public record ReadyUpdate(int readyCount, int totalCount) {
    }

    public record VoteProgress(int votedCount, int totalCount) {
    }
}
