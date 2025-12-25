# ChessNalysis MVP — Architecture & Design Document

**Version:** 1.0  
**Date:** 2025-12-25  
**Authors:** Yashas Chandra
**Scope:** Multiplayer chess MVP (authentication, user identity, matchmaking, game clocks, real-time play)

---

## 1. Overview

This project is a **multiplayer chess web application** with:

- User authentication & identity
- Real-time games using WebSockets
- Matchmaking by time control
- Game clocks managed server-side
- FEN-based board state storage
- React frontend with `react-chessboard` and `chess.js`
- Spring Boot backend using chesslib for rules validation
- PostgreSQL via Supabase free tier for persistence

The **frontend is stateless**, and the backend is authoritative for game state.

## 2. Goals and Constraints

### MVP Goals

1. Users can authenticate and join games.
2. Players can play against each other in real-time.
3. Game clocks (bullet/blitz/rapid/custom) are implemented and authoritative.
4. Backend validates all moves using chesslib.
5. Board state stored as FEN and transmitted to frontend.

### Out of Scope (MVP)

- Play vs bots / AI
- Stockfish-based analysis
- Spectator mode
- Chat
- Rating/ELO
- Rematch flow
- Anti-cheat

## 3. Backend Architecture

### 3.1 Package Structure

com.chessnalysis
│
├── ChessApplication
├── config
│ ├── SecurityConfig
│ ├── WebSocketConfig
│ ├── JwtConfig
│ └── ThreadConfig
├── controller
│ ├── auth/AuthController
│ ├── matchmaking/MatchmakingController
│ └── game/GameWebSocketController
├── service
│ ├── auth/AuthService
│ ├── matchmaking/MatchmakingService
│ ├── game/GameService
│ ├── game/GameEngineService (chesslib wrapper)
│ └── clock/ClockService
├── dao
│ ├── user/UserRepository
│ └── game/GameRepository
├── domain
│ ├── user/User
│ ├── game/Game, GameStatus, Color, TimeControl
│ └── move/Move
├── dto
│ ├── auth, matchmaking, game
├── websocket
│ ├── interceptor/WebSocketAuthInterceptor
│ └── message/GameEvent, MoveMessage
├── exception
│ ├── GlobalExceptionHandler
│ ├── GameException
│ └── AuthException
└── util
├── IdGenerator
└── TimeUtil


### 3.2 Key Components

| Component | Responsibility |
|-----------|----------------|
| AuthService | JWT auth, user validation |
| GameEngineService | Validate moves, check/checkmate/stalemate (chesslib) |
| GameService | Manage game lifecycle, FEN state, moves |
| MatchmakingService | Queue users by time control, create game on match |
| ClockService | Manage time control, calculate remaining time, handle timeouts |
| GameWebSocketController | Real-time move transmission, subscriptions |

### 3.3 Game Lifecycle

CREATED -> MATCHED -> IN_PROGRESS -> FINISHED

- Finished sub-states: CHECKMATE, TIMEOUT, RESIGNATION, DRAW  
- Only backend can transition states.

### 3.4 Persistence (Supabase / Postgres)

**Tables:**

- **users**: id, username, passwordHash, createdAt
- **games**: game_id, white_player_id, black_player_id, current_fen, status, time_control, started_at, finished_at
- **moves** (optional but recommended): game_id, move_number, from, to, promotion, fen, timestamp

## 4. Frontend Architecture

### 4.1 Project Structure

src
├── app (App.tsx, routes.tsx, AuthGuard.tsx)
├── pages (LoginPage, LobbyPage, GamePage)
├── components
│ ├── chess (ChessBoard, MoveHighlighter, PromotionModal)
│ ├── clock (GameClock)
│ └── common (Button, Spinner)
├── services (api.ts, authService.ts, matchmakingService.ts, websocketService.ts)
├── hooks (useWebSocket, useGameState, useAuth)
├── models (GameState, Move, Clock)
├── utils (fen.ts, time.ts)
└── styles (theme.css)

### 4.2 Key Libraries

| Library | Purpose |
|---------|--------|
| react-chessboard | Render chessboard from FEN, drag/drop moves, promotions, animations |
| chess.js | Client-side move validation / legal move highlighting (UI-only) |
| @stomp/stompjs | WebSocket + STOMP for real-time game events |

### 4.3 Frontend Rules

- **FEN is authoritative from backend**.
- Frontend never decides game state.
- chess.js used **only for UX** (highlight legal moves, move previews).
- Game clock interpolated visually from backend timestamps.
- WebSocket reconnect resyncs entire game state.

## 5. Communication Protocol

### REST Endpoints

| Endpoint | Purpose |
|----------|---------|
| POST /auth/login | Authenticate user, return JWT |
| POST /matchmaking/join | Join matchmaking queue |
| GET /games/{gameId} | Fetch current game state (optional) |

### WebSocket

| Flow | Endpoint / Topic |
|------|----------------|
| Client → Server | /app/game/{gameId}/move |
| Server → Client | /topic/game/{gameId} |

**Message Format:**

- **Server → Client**
```json
{
  "type": "GAME_STATE",
  "fen": "...",
  "clock": { "white": 295, "black": 300 },
  "status": "ACTIVE"
}
```
- **Client → Server**
```json
{
  "from": "e2",
  "to": "e4",
  "promotion": null
}
```

## 6. Security

- JWT for REST & WebSocket authentication.
- Only the two players of a game can send moves.
- Future spectators (Phase 2) will be read-only.
- Backend validates all actions to prevent cheating.

## 7. Error Handling & Edge Cases

| Scenario | Handling |
|---------|---------|
Illegal move | Reject, notify player via WebSocket |
Wrong turn | Ignore the move |
WebSocket drop | Reconnect and resync game state |
Opponent disconnect | Clock continues; game may timeout |
Game end | Notify both players; lock state |

## 8. Observability

- Correlation ID per game for tracing.
- Logs:
  - Moves received and rejected
  - Game state transitions
  - Clock expirations
- Metrics (optional MVP):
  - Active games
  - Matchmaking queue size

## 9. Future-proofing for Phase 2

- Frontend libraries (`react-chessboard` + `chess.js`) are ready for:
  - Bots
  - Stockfish analysis
  - Move evaluation & mistake highlighting
- Backend architecture supports:
  - Bot players
  - Engine evaluation
  - Game history analysis
- FEN-based board state ensures **replay, analysis, and spectator support**

## 10. Locked Decisions

| Area | Decision |
|------|---------|
Architecture | Layered Spring Boot (controller/service/dao/domain/dto) |
Chess engine | chesslib (Java, authoritative) |
Frontend | React + react-chessboard + chess.js |
Realtime | STOMP over WebSocket |
FEN | Single source of truth for board state |
Clock | Backend authoritative, frontend interpolated |
Persistence | Supabase/Postgres MVP |
MVP features | Multiplayer only, authentication, matchmaking, game clock |

---

This document represents the **finalized MVP-1 design**.
