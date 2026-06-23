package server.net;

import common.protocol.*;
import common.net.*;
import server.db.ConnectionPool;
import server.db.DbInitializer;
import server.db.dao.GameDao;
import server.db.dao.GameParticipantDao;
import server.db.dao.GameSongDao;
import server.db.dao.RoomDao;
import server.db.dao.RoundDao;
import server.db.dao.SavedPlaylistDao;
import server.db.dao.SongCacheDao;
import server.db.dao.UserDao;
import server.external.HttpJson;
import server.external.PlaylistResolver;
import server.external.SpotifyClient;
import server.external.YouTubeClient;
import server.game.GameManager;
import server.room.RoomManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class GameServer {
    private static final int PORT = 2503;

    public static void main(String[] args) throws IOException {
        CryptoService crypto = new CryptoService("StoreServerTest1".getBytes(StandardCharsets.UTF_8));
        ExecutorService clientThreads = Executors.newCachedThreadPool();

        ConnectionPool pool = new ConnectionPool();
        new DbInitializer(pool).initialize();

        UserDao userDao = new UserDao(pool);
        RoomDao roomDao = new RoomDao(pool);
        RoundDao roundDao = new RoundDao(pool);
        SavedPlaylistDao savedPlaylistDao = new SavedPlaylistDao(pool);
        SongCacheDao songCacheDao = new SongCacheDao(pool);
        HttpJson http = new HttpJson();
        PlaylistResolver playlistResolver = new PlaylistResolver(
                new YouTubeClient(http, System.getenv("YOUTUBE_API_KEY")),
                new SpotifyClient(http, System.getenv("SPOTIFY_CLIENT_ID"), System.getenv("SPOTIFY_CLIENT_SECRET")),
                songCacheDao);
        RoomManager roomManager = new RoomManager();
        SessionManager sessionManager = new SessionManager();
        GameDao gameDao = new GameDao(pool);
        GameParticipantDao participantDao = new GameParticipantDao(pool);
        GameSongDao songDao = new GameSongDao(pool);
        GameManager gameManager = new GameManager();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                clientThreads.submit(
                        () -> handleClient(clientSocket, crypto, userDao, roomDao, roomManager, sessionManager,
                                savedPlaylistDao, playlistResolver, gameDao, participantDao, songDao, gameManager,
                                roundDao));
            }
        }
    }

    private static void handleClient(Socket socket, CryptoService crypto, UserDao userDao, RoomDao roomDao,
            RoomManager roomManager, SessionManager sessionManager,
            SavedPlaylistDao savedPlaylistDao, PlaylistResolver playlistResolver,
            GameDao gameDao, GameParticipantDao participantDao,
            GameSongDao songDao, GameManager gameManager, RoundDao roundDao) {
        ExecutorService pipeline = Executors.newFixedThreadPool(4);
        try {
            MessageReceiver receiver = new SocketMessageReceiver(socket);
            MessageSender sender = new SocketMessageSender(socket);

            BlockingQueue<byte[]> incoming = new LinkedBlockingQueue<>();
            BlockingQueue<Packet> decoded = new LinkedBlockingQueue<>();
            BlockingQueue<Packet> responses = new LinkedBlockingQueue<>();
            BlockingQueue<byte[]> outgoing = new LinkedBlockingQueue<>();

            pipeline.submit(new Decryptor(incoming, decoded, crypto));

            pipeline.submit(new Processor(decoded, responses, userDao, roomDao, roomManager, sessionManager,
                    savedPlaylistDao, playlistResolver, gameDao, participantDao, songDao, gameManager, roundDao));

            pipeline.submit(new Encryptor(responses, outgoing, crypto));
            pipeline.submit(new Sender(sender, outgoing));

            new Receiver(receiver, incoming).run();

        } catch (IOException e) {
            System.err.println("[Server] Error: " + e.getMessage());
        } finally {
            pipeline.shutdownNow();
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            System.out.println("[Server] Client disconnected");
        }
    }
}