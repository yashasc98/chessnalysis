export type LoginRequest = { username: string; password: string; deviceId?: string }
export type RegisterRequest = { username: string; password: string }

export type LoginResponse = {
  token: string
  refreshToken: string
  deviceId: string
  userId: number
  username: string
  role: string
  expiresIn: number
}

export type RegisterResponse = { userId: number; username: string; message: string }

// Matchmaking WS DTOs
export type EnterQueueRequest = { timeControl: string }
export type LeaveQueueRequest = { queueId?: string }

// Game WS DTOs (incoming events)
export type MatchFoundEvent = { 
  gameId: string
  opponentId: number
  opponentUsername: string
  color: string
  timeControl: any 
}

export type GameStartedEvent = { 
  gameId: string
  whitePlayerId: number
  blackPlayerId: number
  timeControl: string 
}

export type MoveAppliedEvent = { 
  gameId: string
  moveUci: string
  sanNotation: string
  byColor: string
  moveNumber: number
  fen: string
  clock: any 
}

export type GameStateSyncEvent = { 
  gameId: string
  state: string
  fen: string
  moveCount: number
  clock: any
  result?: any
  resultReason?: string
  whitePlayerId: number
  blackPlayerId: number
}

export type IllegalMoveEvent = { 
  gameId: string
  moveUci: string
  reason: string 
}

export type GameEndedEvent = { 
  gameId: string
  result: string
  reason: string
  finishedAt: string 
}
