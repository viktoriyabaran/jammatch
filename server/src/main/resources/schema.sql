CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nickname TEXT NOT NULL,
    client_token TEXT UNIQUE
);

CREATE TABLE IF NOT EXISTS rooms (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    room_code TEXT NOT NULL UNIQUE,
    room_name TEXT NOT NULL,
    host_id INTEGER NOT NULL,
    max_players INTEGER DEFAULT 10,
    total_rounds INTEGER DEFAULT 5,
    round_duration_sec INTEGER DEFAULT 30,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (host_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS games (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    room_id INTEGER NOT NULL,
    status TEXT NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE TABLE IF NOT EXISTS game_participants (
    game_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    playlist_url TEXT,
    score INTEGER DEFAULT 0,
    PRIMARY KEY (game_id, user_id),
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS rounds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    game_id INTEGER NOT NULL,
    track_id TEXT NOT NULL,
    correct_user_id INTEGER NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (correct_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS banned_artists (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

INSERT OR IGNORE INTO banned_artists (name) VALUES 
('Morgenshtern'), ('Instasamka'), ('Basta'), ('Oxxxymiron'), ('Egor Kreed'), 
('Zivert'), ('Timati'), ('Polina Gagarina'), ('Leningrad'), ('Macan'),
('Miyagi & Andy Panda'), ('Anna Asti'), ('JONY'), ('HammAli & Navai'), ('Artik & Asti'),
('Zemfira'), ('Kasta'), ('Niletto'), ('Slava Marlow'), ('Pharaoh'),
('Guf'), ('Dzharakhov'), ('Skryptonite'), ('LSP'), ('Max Korzh'),
('Dabro'), ('Ramil'''), ('Mary Gu'), ('Klava Koka'), ('Gayazovs Brothers'),
('Lyube'), ('Grigory Leps'), ('Stas Mikhailov'), ('Valeriya'), ('Oleg Gazmanov'),
('Nikolay Baskov'), ('Philipp Kirkorov'), ('Dima Bilan'), ('Sergey Lazarev'), ('Valery Meladze'),
('Mot'), ('L''One'), ('T-Fest'), ('Eldzhey'), ('Face'),
('Kish'), ('Bi-2'), ('Splean'), ('DDT'), ('Aria');

CREATE TABLE IF NOT EXISTS song_cache (
    video_id TEXT PRIMARY KEY,
    title TEXT,
    artist TEXT,
    cover_url TEXT,
    matched INTEGER NOT NULL,
    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS saved_playlists (
    user_id INTEGER NOT NULL,
    url TEXT NOT NULL,
    name TEXT,
    track_count INTEGER,
    cover_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, url),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS game_songs (
    game_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    video_id TEXT NOT NULL,
    PRIMARY KEY (game_id, user_id, video_id),
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);