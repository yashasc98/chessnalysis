# ChessNalysis MVP

**Version:** 1.0  
**Date:** 2025-12-25  
**Authors:** Yashas Chandra

---

## Overview

This project is a **multiplayer chess web application** (MVP) with:

- User authentication & identity
- Real-time games using WebSockets
- Matchmaking by time control
- Game clocks managed server-side
- FEN-based board state storage
- React frontend using `react-chessboard` and `chess.js`
- Spring Boot backend using `chesslib` for move validation
- PostgreSQL via Supabase free tier for persistence

---

## Tech Stack

- **Frontend:** React.js, react-chessboard, chess.js, STOMP/WebSocket
- **Backend:** Java, Spring Boot, chesslib
- **Database:** Supabase (PostgreSQL)
- **Authentication:** JWT
- **Real-time:** WebSockets with STOMP protocol

---

## Features (MVP)

- User login and authentication
- Multiplayer games (1v1)
- Matchmaking by time control
- Backend-authoritative game state and clock
- FEN-based board state updates via WebSocket
- Reconnect and resync for dropped connections

---

## Getting Started

### Prerequisites

- Java 21+
- Node.js 18+ (for frontend)
- Maven or Gradle
- Supabase account (free tier)
- npm or yarn

### Backend Setup

1. Clone the repo:
```bash
git clone <repo-url>
cd backend
```

2. Configure Supabase connection in application.yml:
```
spring:
  datasource:
    url: jdbc:postgresql://<supabase-host>:5432/<db>
    username: <user>
    password: <password>
```

3. Run the backend:
```
./mvnw spring-boot:run
```

### Backend Setup

1. Navigate to frontend:
```
cd frontend
```

2. Install dependencies:
```
npm install
```
or
```
yarn install
```

3. Run frontend:
```
npm start
```
or
```
yarn start
```

4. Open your browser at http://localhost:3000.

### Project Structure
#### Backend

Layered Spring Boot structure:
```
controller, service, dao, domain, dto, websocket, config, util, exception
```

#### Frontend

React functional components
```
Folders: pages, components, services, hooks, models, utils, styles
```

### Contributing

1. Fork the repo and create a feature branch
2. Run tests locally
3. Submit a pull request

---
