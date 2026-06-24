package client.ui;

import com.sun.net.httpserver.HttpServer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.media.AudioClip;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;

import java.io.OutputStream;
import java.net.InetSocketAddress;

public class AudioService {

    public static final int DEFAULT_OFFSET = 30;

    private static final double CRACKLE_VOLUME = 0.3;
    private static final Duration CRACKLE_FADE = Duration.millis(900);

    private static final int INTRO_FADE_TARGET = 12;
    private static final int INTRO_FADE_MS = 2000;

    private WebView view;
    private WebEngine engine;
    private HttpServer server;
    private boolean started;
    private boolean pageLoaded;
    private String pendingId;
    private int pendingOffset;

    private AudioClip crackle;
    private boolean crackleActive;
    private Timeline crackleFade;
    private boolean introFadeDone;

    public Node view() {
        ensureStarted();
        return view;
    }

    public void warmUp() {
        ensureStarted();
    }

    public void play(String videoId, int offset) {
        ensureStarted();
        if (videoId == null || videoId.isBlank()) {
            return;
        }
        introFadeDone = false;
        startCrackle();
        if (!pageLoaded) {
            pendingId = videoId;
            pendingOffset = offset;
            return;
        }
        loadTrack(videoId, offset);
    }

    public void pause() {
        stopCrackle();
        if (pageLoaded) {
            engine.executeScript("pauseTrack()");
        }
    }

    public void stop() {
        pendingId = null;
        stopCrackle();
        if (pageLoaded) {
            engine.executeScript("stopTrack()");
        }
    }

    public void dispose() {
        stopCrackle();
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (engine != null) {
            engine.load(null);
        }
        started = false;
        pageLoaded = false;
    }

    private void startCrackle() {
        if (crackle == null) {
            crackle = new AudioClip(getClass().getResource("/audio/crackle.wav").toExternalForm());
            crackle.setCycleCount(AudioClip.INDEFINITE);
        }
        if (crackleFade != null) {
            crackleFade.stop();
            crackleFade = null;
        }
        crackle.setVolume(CRACKLE_VOLUME);
        if (!crackleActive) {
            crackle.play();
            crackleActive = true;
        }
    }

    private void fadeOutCrackle() {
        if (!crackleActive || crackle == null) {
            return;
        }
        if (crackleFade != null) {
            crackleFade.stop();
        }
        crackleFade = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(crackle.volumeProperty(), crackle.getVolume())),
                new KeyFrame(CRACKLE_FADE, new KeyValue(crackle.volumeProperty(), 0.0)));
        crackleFade.setOnFinished(e -> stopCrackle());
        crackleFade.play();
    }

    private void stopCrackle() {
        if (crackleFade != null) {
            crackleFade.stop();
            crackleFade = null;
        }
        if (crackle != null) {
            crackle.stop();
        }
        crackleActive = false;
    }

    private void loadTrack(String videoId, int offset) {
        engine.executeScript("loadTrack('" + videoId + "', " + offset + ")");
    }

    private void ensureStarted() {
        if (started) {
            return;
        }
        started = true;

        crackle = new AudioClip(getClass().getResource("/audio/crackle.wav").toExternalForm());
        crackle.setCycleCount(AudioClip.INDEFINITE);

        int port = startServer();

        view = new WebView();
        view.setManaged(false);
        view.resize(1, 1);
        view.setOpacity(0);
        view.setMouseTransparent(true);

        engine = view.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("bridge", new Bridge());
                pageLoaded = true;
                if (pendingId != null) {
                    String id = pendingId;
                    pendingId = null;
                    loadTrack(id, pendingOffset);
                }
            }
        });

        engine.load("http://localhost:" + port + "/audio/player.html");
    }

    private int startServer() {
        try {
            byte[] html;
            try (var in = getClass().getResourceAsStream("/audio/player.html")) {
                html = in.readAllBytes();
            }
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/audio/player.html", exchange -> {
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, html.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(html);
                }
            });
            server.start();
            return server.getAddress().getPort();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start audio player server", e);
        }
    }

    public class Bridge {
        public void onLog(String msg) {
            System.out.println("[Audio] " + msg);
        }

        public void onReady() {
            System.out.println("[Audio] player ready");
        }

        public void onState(String state) {
            if ("1".equals(state)) {
                fadeOutCrackle();
                if (!introFadeDone) {
                    introFadeDone = true;
                    engine.executeScript("fadeTo(" + INTRO_FADE_TARGET + ", " + INTRO_FADE_MS + ")");
                }
            }
        }

        public void onError(String code) {
            System.out.println("[Audio] error " + code);
            stopCrackle();
        }
    }
}
