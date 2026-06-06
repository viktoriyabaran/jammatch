Messages come in two kinds:

- **Requests** (client to server) get one reply, which matches to the
  request by its packet id (bPktId). The reply carries a ResponseCode status
  (and any return data).
- **Events** (server to client) are pushed by the server on their own, with no
  request behind them (a round starting on a timer, the lobby changing).

## Commands

| Command              | Code | Kind    | Purpose                           |
| -------------------- | ---- | ------- | --------------------------------- |
| CLIENT_LOGIN       | 100  | request | Log in with a nickname            |
| SUBMIT_PLAYLIST    | 101  | request | Provide a public YouTube playlist |
| CREATE_ROOM        | 200  | request | Create a new room                 |
| JOIN_ROOM          | 201  | request | Join a room by code               |
| LEAVE_ROOM         | 202  | request | Leave the current room            |
| UPDATE_ROOM_CONFIG | 203  | request | Host edits room settings          |
| KICK_PLAYER        | 204  | request | Host removes a player             |
| LOBBY_UPDATE       | 210  | event   | Full, current room state          |
| ROOM_CLOSED        | 211  | event   | The room was closed               |
| START_GAME         | 301  | request | Host starts the game              |
| SUBMIT_VOTE        | 302  | request | Player's guess for the round      |
| ROUND_START        | 310  | event   | New round: track + options        |
| ROUND_END          | 311  | event   | Round result + scores             |
| GAME_OVER          | 312  | event   | Final leaderboard                 |


## Response codes

Carried in the reply to a request.

| Code | Name                   | Meaning                                  |
|------|------------------------|------------------------------------------|
| 200  | OK                   | Request succeeded                        |
| 400  | INVALID_PLAYLIST     | Playlist URL is malformed or unreadable  |
| 401  | LOGIN_FAILED         | Login was rejected                       |
| 403  | NOT_HOST             | Action is host-only                      |
| 404  | ROOM_NOT_FOUND       | No room with that code                   |
| 405  | CANNOT_KICK_HOST     | The host cannot be kicked                |
| 409  | ROOM_FULL            | Room is at capacity                      |
| 422  | NICKNAME_TAKEN       | Nickname already in use                  |
| 423  | GAME_ALREADY_STARTED | Game is in progress, action not allowed  |
| 500  | INTERNAL_ERROR       | Unexpected server error                  |

## Request payloads

| Command              | Payload                                                       | Reply data     |
|----------------------|---------------------------------------------------------------|----------------|
| CLIENT_LOGIN       | { nickname }                                                | user id        |
| SUBMIT_PLAYLIST    | { playlistUrl }                                             | —              |
| CREATE_ROOM        | { roomName, maxPlayers, rounds, roundDurationSeconds }      | { roomCode } |
| JOIN_ROOM          | { roomCode }                                                | —              |
| LEAVE_ROOM         | { }                                                         | —              |
| UPDATE_ROOM_CONFIG | { roomName, maxPlayers, rounds, roundDurationSeconds }      | —              |
| KICK_PLAYER        | { targetUserId }                                            | —              |
| START_GAME         | { }                                                         | —              |
| SUBMIT_VOTE        | { roundNumber, votedUserId }                                | —              |

## Event payloads

| Event          | Payload                                                                                                                             |
|----------------|-------------------------------------------------------------------------------------------------------------------------------------|
| LOBBY_UPDATE | { roomCode, roomName, settings { maxPlayers, rounds, roundDurationSeconds }, hostId, players: [ { id, nickname, hasPlaylist } ] } |
| ROOM_CLOSED  | { reason }                                                                                                                        |
| ROUND_START  | { roundNumber, totalRounds, videoId, durationSeconds, options: [ userId ] }                                                       |
| ROUND_END    | { roundNumber, correctUserId, results: [ { userId, votedUserId, correct, roundPoints, totalScore } ] }                            |
| GAME_OVER    | { leaderboard: [ { userId, nickname, score, rank } ] }                                                                            |
