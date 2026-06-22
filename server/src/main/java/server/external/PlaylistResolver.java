package server.external;

import server.db.dao.SongCacheDao;
import server.external.model.AlbumCover;
import server.external.model.CachedSong;
import server.external.model.PlaylistPreview;
import server.external.model.ResolvedTrack;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlaylistResolver {
    private final YouTubeClient youTube;
    private final SpotifyClient spotify;
    private final SongCacheDao songCache;

    public PlaylistResolver(YouTubeClient youTube, SpotifyClient spotify, SongCacheDao songCache) {
        this.youTube = youTube;
        this.spotify = spotify;
        this.songCache = songCache;
    }

    public PlaylistPreview preview(String playlistUrl) throws IOException, InterruptedException {
        List<ResolvedTrack> tracks = youTube.fetchTracks(playlistUrl);
        String name = youTube.fetchPlaylistTitle(playlistUrl);
        String cover = tracks.isEmpty() ? null : tracks.get(0).youtubeThumbnail();
        return new PlaylistPreview(name, tracks.size(), cover);
    }

    public List<String> resolve(String playlistUrl) throws IOException, InterruptedException {
        List<ResolvedTrack> tracks = youTube.fetchTracks(playlistUrl);

        List<String> videoIds = new ArrayList<>();
        for (ResolvedTrack track : tracks) {
            try {
                resolveSong(track);
                videoIds.add(track.videoId());
            } catch (IOException | SQLException e) {
                System.err.println("[PlaylistResolver] skipping " + track.videoId() + ": " + e.getMessage());
            }
        }
        return videoIds;
    }

    private CachedSong resolveSong(ResolvedTrack track) throws IOException, InterruptedException, SQLException {
        var cached = songCache.find(track.videoId());
        if (cached.isPresent()) {
            return cached.get();
        }

        AlbumCover cover = spotify.findAlbumCover(track.title(), track.channel());

        CachedSong song;
        if (cover != null) {
            song = new CachedSong(track.videoId(), cover.trackName(), cover.artistName(), cover.coverUrl(), true);
        } else {
            String artist = AlbumCoverMatcher.cleanChannel(track.channel());
            if (artist.isBlank()) {
                artist = track.channel();
            }
            song = new CachedSong(track.videoId(), track.title(), artist, track.youtubeThumbnail(), false);
        }

        songCache.save(song);
        return song;
    }
}
