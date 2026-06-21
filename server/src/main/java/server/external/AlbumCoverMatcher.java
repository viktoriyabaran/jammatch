package server.external;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AlbumCoverMatcher {

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}']+");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "of", "and", "or", "feat", "ft",
            "official", "video", "audio", "lyrics", "music");

    private static final String[] TITLE_NOISE = {
            "official video", "official music video", "official audio", "lyric video",
            "lyrics", "music video", "audio", "hd", "4k", "remastered", "official"};

    private static final String[] SEPARATORS = {" - ", " — ", " – ", ": ", " | "};

    private AlbumCoverMatcher() {
    }

    public static double matchScore(String spotifyTrack, List<String> spotifyArtists, String targetSong, String targetArtist) {
        Set<String> songWords = words(targetSong);
        if (songWords.isEmpty()) {
            return 0.0;
        }
        Set<String> spotifySongWords = words(spotifyTrack);

        Set<String> sharedSongWords = new HashSet<>(songWords);
        sharedSongWords.retainAll(spotifySongWords);
        if (sharedSongWords.isEmpty()) {
            return 0.0;
        }
        double songScore = similarity(songWords, spotifySongWords);

        double artistScore = 0.5;
        if (targetArtist != null && !targetArtist.isBlank()) {
            Set<String> artistWords = words(targetArtist);
            Set<String> spotifyArtistWords = new HashSet<>();
            for (String a : spotifyArtists) {
                spotifyArtistWords.addAll(words(a));
            }
            if (!artistWords.isEmpty() && !spotifyArtistWords.isEmpty()) {
                artistScore = similarity(artistWords, spotifyArtistWords);
                if (artistScore == 0.0) {
                    return 0.0;
                }
            }
        }
        return 0.7 * songScore + 0.3 * artistScore;
    }

    public static String[] splitTitle(String title) {
        String clean = cleanTitle(title);
        for (String sep : SEPARATORS) {
            int i = clean.indexOf(sep);
            if (i > 0) {
                String artist = clean.substring(0, i).strip();
                String song = clean.substring(i + sep.length()).strip();
                if (!artist.isEmpty() && !song.isEmpty()) {
                    return new String[]{artist, song};
                }
            }
        }
        return new String[]{"", clean};
    }

    public static String cleanChannel(String channel) {
        if (channel == null) {
            return "";
        }
        String c = channel.replaceAll("(?i)\\s*-\\s*Topic\\s*$", "");
        c = c.replaceAll("(?i)VEVO$", "");
        c = c.replaceAll("(?i)\\s*(Official|Music|Records|Channel)\\s*$", "");
        return c.strip();
    }

    public static String cleanTitle(String title) {
        if (title == null) {
            return "";
        }
        String t = title.replaceAll("[\\(\\[].*?[\\)\\]]", "");
        for (String word : TITLE_NOISE) {
            t = t.replaceAll("(?i)\\b" + Pattern.quote(word) + "\\b", "");
        }
        return t.replaceAll("\\s+", " ").replaceAll("^[\\s-]+|[\\s-]+$", "");
    }

    private static double similarity(Set<String> a, Set<String> b) {
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        if (union.isEmpty()) {
            return 0.0;
        }
        Set<String> intersect = new HashSet<>(a);
        intersect.retainAll(b);
        return (double) intersect.size() / union.size();
    }

    private static Set<String> words(String s) {
        if (s == null || s.isBlank()) {
            return Set.of();
        }
        String normalized = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFKD);
        normalized = MARKS.matcher(normalized).replaceAll("");

        Set<String> words = new HashSet<>();
        Matcher matcher = WORD.matcher(normalized);
        while (matcher.find()) {
            String word = matcher.group();
            if (word.length() > 1 && !STOPWORDS.contains(word)) {
                words.add(word);
            }
        }
        return words;
    }

    public static void main(String[] args) {
        System.out.println(matchScore("Love Me or Hate Me",
                List.of("Song Soowoo"), "Love Me or Hate Me", "Song Soowoo"));
        System.out.println(matchScore("DNA",
                List.of("Kendrick Lamar"), "Love Me or Hate Me", "Song Soowoo"));
    }
}
