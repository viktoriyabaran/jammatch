# JamMatch

A multiplayer game in which players connect their YouTube playlists; each round
a random track plays and players have to guess which participant's playlist it
belongs to.

**Team:** Viktoriia Baran, Sofiia Zakharuk

## Feature set

- Simple nickname-based login (no passwords)
- Room creation via a 6-digit code; a player lobby with real-time updates
- A player pastes a link to a public YouTube playlist
- YouTube Data API integration to fetch tracks and metadata, and to verify that a track is embeddable
- Spotify Web API integration to fetch album covers
- Synchronized playback through the YouTube IFrame Player inside a JavaFX WebView
- "Whose track is it?" voting, with points awarded for speed and accuracy
- Final leaderboard
- Filtering of Russian artists via a blacklist in the database
- Persistence of game statistics

## User roles

| Role          | Capabilities                                                        |
|---------------|---------------------------------------------------------------------|
| **Player**    | Join a room by code, provide a public YouTube playlist, and play.   |
| **Room host** | All player actions, plus create a room, set its rules (number of rounds, round duration), and remove players. |

## Usage flows

### Flow 1: Joining a room

1. Enter a nickname
2. Enter the room code
3. Paste a link to a public YouTube playlist
4. Wait in the lobby for the game to start

### Flow 2: Creating a room (host)

1. Enter a nickname
2. "Create room" and choose its parameters (name, number of rounds, round duration)
3. Receive a 6-digit code
4. Friends join using the code; the host starts the game

### Flow 3: Round progression

1. The server picks a random track from the combined set of playlists
2. A round-start command is sent to all clients synchronously
3. The track plays for everyone at the same time
4. Players vote "whose track is it?" (including the owner — symmetrically)
5. The server records the timestamp of each vote
6. The round ends with results and a scoreboard
7. After N rounds, the game ends and a final leaderboard is shown

---

## Modules

Maven multi-module project, Java 25.

| Module   | Description                                                          |
|----------|---------------------------------------------------------------------|
| `common` | Shared secure packet protocol and message types (used by both sides). |
| `server` | Game server: rooms, lobby, game engine, database.                   |
| `client` | JavaFX desktop client.                                              |
| `docs/`  | Protocol, architecture, and database documentation.                 |

## Build

Requires JDK 25+ and Maven 3.9+. From the repository root, run `mvn clean install`.

## Documentation

- [Protocol command reference](docs/protocol.md)
- [Architecture](docs/architecture.md)
- [Database schema](docs/db-schema.md)