import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { Chess } from 'chess.js'
import { Chessboard } from 'react-chessboard'
import { stompClient } from '../ws/stompClient'
import { useAuth } from '../contexts/AuthContext'
import type {
  MoveAppliedEvent,
  GameStateSyncEvent,
  IllegalMoveEvent,
  GameStartedEvent,
  GameEndedEvent,
  MatchFoundEvent
} from '../types'

interface GameState {
  gameId: string | undefined
  status: 'PENDING' | 'ACTIVE' | 'ENDED' | 'LOADING'
  fen: string
  currentTurn: 'w' | 'b'
  myColor: 'white' | 'black' | null
  opponentId: number | null
  opponentUsername: string | null
  whitePlayerId: number | null
  blackPlayerId: number | null
  result: string | null
  resultReason: string | null
  lastError: string | null
}

interface GameLocationState {
  matchEvent: MatchFoundEvent
}

export default function Game() {
  const { gameId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuth()

  const chessRef = useRef(new Chess())
  const [gameState, setGameState] = useState<GameState>(() => {
    const state = location.state as GameLocationState | null
    const matchEvent = state?.matchEvent

    return {
      gameId,
      status: 'LOADING',
      fen: chessRef.current.fen(),
      currentTurn: 'w',
      myColor: matchEvent?.color === 'WHITE' ? 'white' : matchEvent?.color === 'BLACK' ? 'black' : null,
      opponentId: matchEvent?.opponentId || null,
      opponentUsername: matchEvent?.opponentUsername || null,
      whitePlayerId: null,
      blackPlayerId: null,
      result: null,
      resultReason: null,
      lastError: null
    }
  })

  const [isMyTurn, setIsMyTurn] = useState(false)
  const [moveHistory, setMoveHistory] = useState<string[]>([])
  const subscriptionsRef = useRef<any[]>([])

  // Setup subscriptions and sync on mount
  useEffect(() => {
    if (!gameId || !user) return

    const setupGame = async () => {
      try {
        await stompClient.waitForConnection()

        // Subscribe to game topic for all players
        const gameTopic = `/topic/game.${gameId}`
        const gameSub = stompClient.subscribe(gameTopic, (msg) => {
          const payload = JSON.parse(msg.body)
          handleGameEvent(payload)
        })
        subscriptionsRef.current.push(gameSub)

        // Subscribe to personal game-sync queue for reconnection
        const syncSub = stompClient.subscribe('/user/queue/game-sync', (msg) => {
          const payload: GameStateSyncEvent = JSON.parse(msg.body)
          if (payload.gameId === gameId) {
            applyGameSync(payload)
          }
        })
        subscriptionsRef.current.push(syncSub)

        // Subscribe to error queue
        const errorSub = stompClient.subscribe('/user/queue/errors', (msg) => {
          const payload = JSON.parse(msg.body)
          setGameState((s) => ({ ...s, lastError: payload.message }))
        })
        subscriptionsRef.current.push(errorSub)

        // Request sync from server (in case we're reconnecting)
        await stompClient.send(`/app/game/${gameId}/sync`, {})

        // Set initial status to PENDING (waiting for opponent or game start)
        setGameState((s) => ({ ...s, status: 'PENDING' }))
      } catch (e) {
        console.error('Failed to setup game:', e)
        setGameState((s) => ({ ...s, lastError: 'Failed to connect to game' }))
      }
    }

    setupGame()

    return () => {
      subscriptionsRef.current.forEach((sub) => {
        if (sub && typeof sub.unsubscribe === 'function') {
          sub.unsubscribe()
        }
      })
      subscriptionsRef.current = []
    }
  }, [gameId, user])

  // Determine if it's my turn
  useEffect(() => {
    if (!user || !gameState.myColor || gameState.status !== 'ACTIVE') {
      setIsMyTurn(false)
      return
    }

    const myColorChar = gameState.myColor === 'white' ? 'w' : 'b'
    setIsMyTurn(gameState.currentTurn === myColorChar)
  }, [gameState.currentTurn, gameState.myColor, gameState.status, user])

  function handleGameEvent(payload: any) {
    console.log('🎮 Game event received:', JSON.stringify(payload, null, 2))
    // Determine event type by checking which fields are present
    if ((payload as GameStartedEvent).whitePlayerId !== undefined) {
      const event = payload as GameStartedEvent
      setGameState((s) => ({
        ...s,
        status: 'ACTIVE',
        whitePlayerId: event.whitePlayerId,
        blackPlayerId: event.blackPlayerId
      }))
      console.log('Game started:', event)
    } else if ((payload as IllegalMoveEvent).reason !== undefined) {
      // Check for IllegalMoveEvent BEFORE MoveAppliedEvent since both have moveUci
      const event = payload as IllegalMoveEvent
      setGameState((s) => ({ ...s, lastError: `Illegal move: ${event.reason}` }))
      // Revert the board to the last valid FEN
      chessRef.current.undo()
    } else if ((payload as MoveAppliedEvent).moveUci !== undefined) {
      applyMoveEvent(payload as MoveAppliedEvent)
    } else if ((payload as GameEndedEvent).result !== undefined) {
      const event = payload as GameEndedEvent
      setGameState((s) => ({
        ...s,
        status: 'ENDED',
        result: event.result,
        resultReason: event.reason
      }))
    }
  }

  function applyMoveEvent(event: MoveAppliedEvent) {
    try {
      console.log('📍 Applying move event:', { moveUci: event.moveUci, fen: event.fen, san: event.sanNotation })
      if (!event.fen) {
        console.error('❌ MoveAppliedEvent missing FEN field:', event)
        return
      }
      chessRef.current.load(event.fen)
      setGameState((s) => ({
        ...s,
        fen: event.fen,
        currentTurn: chessRef.current.turn()
      }))
      setMoveHistory((prev) => [...prev, event.sanNotation])
      console.log('✅ Move applied successfully:', event.sanNotation)
    } catch (e) {
      console.error('Failed to load FEN from move event:', e)
      setGameState((s) => ({ ...s, lastError: 'Failed to apply move' }))
    }
  }

  function applyGameSync(event: GameStateSyncEvent) {
    try {
      chessRef.current.load(event.fen)
      setGameState((s) => ({
        ...s,
        fen: event.fen,
        currentTurn: chessRef.current.turn(),
        status: event.state as 'PENDING' | 'ACTIVE' | 'ENDED'
      }))
      console.log('Game synced from server:', event)
    } catch (e) {
      console.error('Failed to load FEN from sync event:', e)
    }
  }

  async function handleStartGame() {
    if (!gameId) return
    try {
      await stompClient.send(`/app/game/${gameId}/start`, {})
      console.log('Start game signal sent')
    } catch (e) {
      console.error('Failed to start game:', e)
      setGameState((s) => ({ ...s, lastError: 'Failed to start game' }))
    }
  }

  function onPieceDrop(args: any): boolean {
    const { sourceSquare, targetSquare } = args

    // Can't move if not my turn or game not active
    if (!isMyTurn || gameState.status !== 'ACTIVE') {
      console.warn('Not your turn or game not active')
      return false
    }

    if (!sourceSquare || !targetSquare) return false

    try {
      // Attempt move with standard promotion to queen (can be enhanced)
      const move = chessRef.current.move({
        from: sourceSquare,
        to: targetSquare,
        promotion: 'q'
      })

      if (!move) {
        console.warn('Illegal move:', sourceSquare, targetSquare)
        return false
      }

      // Update local board state optimistically
      setGameState((s) => ({
        ...s,
        fen: chessRef.current.fen(),
        currentTurn: chessRef.current.turn()
      }))

      // Send move to backend (format: source + target + optional promotion)
      const moveUci = sourceSquare + targetSquare + (move.promotion ? move.promotion : '')
      sendMove(moveUci)

      return true
    } catch (e) {
      console.error('Error handling piece drop:', e)
      return false
    }
  }

  async function sendMove(moveUci: string) {
    if (!gameId) return
    try {
      console.log(`Sending move: ${moveUci}`)
      await stompClient.send(`/app/game/${gameId}/move`, { moveUci })
    } catch (e) {
      console.error('Failed to send move:', e)
      // Undo the move optimistically applied
      chessRef.current.undo()
      setGameState((s) => ({
        ...s,
        fen: chessRef.current.fen(),
        currentTurn: chessRef.current.turn(),
        lastError: 'Failed to send move to server'
      }))
    }
  }

  async function handleResign() {
    if (!gameId) return
    try {
      await stompClient.send(`/app/game/${gameId}/resign`, {})
      console.log('Resignation sent')
    } catch (e) {
      console.error('Failed to resign:', e)
      setGameState((s) => ({ ...s, lastError: 'Failed to resign' }))
    }
  }

  function getOpponentInfo() {
    if (gameState.opponentUsername) {
      return `${gameState.opponentUsername} (ID: ${gameState.opponentId})`
    }
    return 'Opponent'
  }

  return (
    <div style={styles.container}>
      <h2>Game {gameId}</h2>

      {/* Game Status Section */}
      <div style={styles.statusSection}>
        <div style={styles.playerInfo}>
          <strong>You:</strong> {user?.username} ({gameState.myColor?.toUpperCase()})
        </div>
        <div style={styles.playerInfo}>
          <strong>Opponent:</strong> {getOpponentInfo()}
        </div>
        <div style={styles.status}>
          <strong>Status:</strong> {gameState.status}
        </div>
        {gameState.status === 'ACTIVE' && (
          <div style={styles.turnIndicator}>
            <strong>Current Turn:</strong> {gameState.currentTurn === 'w' ? 'White' : 'Black'}
            {isMyTurn && <span style={styles.myTurn}> (Your Turn!)</span>}
          </div>
        )}
        {gameState.status === 'ENDED' && (
          <div style={styles.result}>
            <strong>Result:</strong> {gameState.result} - {gameState.resultReason}
          </div>
        )}
      </div>

      {/* Error Message */}
      {gameState.lastError && (
        <div style={styles.error}>{gameState.lastError}</div>
      )}

      {/* Move History */}
      {moveHistory.length > 0 && (
        <div style={styles.moveHistory}>
          <strong>Moves:</strong> {moveHistory.join(', ')}
        </div>
      )}

      {/* Chessboard */}
      <div style={styles.boardContainer}>
        <Chessboard
          options={{
            position: gameState.fen,
            onPieceDrop,
            boardOrientation: gameState.myColor === 'black' ? 'black' : 'white'
          }}
        />
      </div>

      {/* Game Controls */}
      <div style={styles.controls}>
        {gameState.status === 'PENDING' && (
          <button onClick={handleStartGame} style={styles.button}>
            Start Game
          </button>
        )}
        {gameState.status === 'ACTIVE' && (
          <button onClick={handleResign} style={styles.button}>
            Resign
          </button>
        )}
        <button onClick={() => navigate('/lobby')} style={styles.button}>
          Back to Lobby
        </button>
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    padding: '20px',
    maxWidth: '800px',
    margin: '0 auto',
    color: '#e0e0e0'
  },
  statusSection: {
    marginBottom: '20px',
    padding: '10px',
    backgroundColor: '#2a2a2a',
    borderRadius: '5px',
    color: '#e0e0e0'
  },
  playerInfo: {
    margin: '5px 0',
    fontSize: '14px',
    color: '#e0e0e0'
  },
  status: {
    margin: '5px 0',
    fontSize: '14px',
    fontWeight: 'bold',
    color: '#e0e0e0'
  },
  turnIndicator: {
    margin: '10px 0',
    padding: '5px',
    backgroundColor: '#1e3a5f',
    borderRadius: '3px',
    fontSize: '14px',
    color: '#e0e0e0'
  },
  myTurn: {
    color: '#64b5f6',
    fontWeight: 'bold'
  },
  result: {
    margin: '10px 0',
    padding: '10px',
    backgroundColor: '#2d4a2d',
    borderRadius: '3px',
    fontSize: '14px',
    color: '#81c784'
  },
  moveHistory: {
    marginBottom: '20px',
    padding: '10px',
    backgroundColor: '#3a3a2a',
    borderRadius: '5px',
    fontSize: '14px',
    color: '#e0e0e0'
  },
  error: {
    marginBottom: '20px',
    padding: '10px',
    backgroundColor: '#4a2a2a',
    color: '#ef5350',
    borderRadius: '5px',
    fontSize: '14px'
  },
  boardContainer: {
    marginBottom: '20px',
    width: '500px',
    height: '500px'
  },
  controls: {
    display: 'flex',
    gap: '10px'
  },
  button: {
    padding: '10px 20px',
    backgroundColor: '#1976d2',
    color: 'white',
    border: 'none',
    borderRadius: '5px',
    cursor: 'pointer',
    fontSize: '14px'
  }
}


