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

  useEffect(() => {
    if (!gameId || !user) return

    const setupGame = async () => {
      try {
        await stompClient.waitForConnection()

        const gameTopic = `/topic/game.${gameId}`
        const gameSub = stompClient.subscribe(gameTopic, (msg) => {
          const payload = JSON.parse(msg.body)
          handleGameEvent(payload)
        })
        subscriptionsRef.current.push(gameSub)

        const syncSub = stompClient.subscribe(`/topic/user.${user?.userId}.sync`, (msg) => {
          const payload: GameStateSyncEvent = JSON.parse(msg.body)
          if (payload.gameId === gameId) {
            applyGameSync(payload)
          }
        })
        subscriptionsRef.current.push(syncSub)

        const errorSub = stompClient.subscribe(`/topic/user.${user?.userId}.errors`, (msg) => {
          const payload = JSON.parse(msg.body)
          setGameState((s) => ({ ...s, lastError: payload.message }))
        })
        subscriptionsRef.current.push(errorSub)

        await stompClient.send(`/app/game/${gameId}/sync`, {})
        setGameState((s) => ({ ...s, status: 'PENDING' }))
      } catch (e) {
        console.error('Failed to setup game:', e)
        setGameState((s) => ({ ...s, lastError: 'Failed to connect to game' }))
      }
    }

    setupGame()

    return () => {
      subscriptionsRef.current.forEach((sub) => sub?.unsubscribe?.())
      subscriptionsRef.current = []
    }
  }, [gameId, user])

  useEffect(() => {
    if (!user || !gameState.myColor || gameState.status !== 'ACTIVE') {
      setIsMyTurn(false)
      return
    }

    const myColorChar = gameState.myColor === 'white' ? 'w' : 'b'
    setIsMyTurn(gameState.currentTurn === myColorChar)
  }, [gameState.currentTurn, gameState.myColor, gameState.status, user])

  function handleGameEvent(payload: any) {
    if ((payload as GameStartedEvent).whitePlayerId !== undefined) {
      const event = payload as GameStartedEvent
      setGameState((s) => ({
        ...s,
        status: 'ACTIVE',
        whitePlayerId: event.whitePlayerId,
        blackPlayerId: event.blackPlayerId
      }))
    } else if ((payload as IllegalMoveEvent).reason !== undefined) {
      const event = payload as IllegalMoveEvent
      setGameState((s) => ({ ...s, lastError: `Illegal move: ${event.reason}` }))
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

  function getMovePairs(): Array<{ moveNumber: number; white: string; black?: string }> {
    const pairs = []
    for (let i = 0; i < moveHistory.length; i += 2) {
      const moveNumber = i / 2 + 1
      pairs.push({
        moveNumber,
        white: moveHistory[i],
        black: moveHistory[i + 1]
      })
    }
    return pairs
  }

  function applyMoveEvent(event: MoveAppliedEvent) {
    try {
      if (!event.fen) {
        console.error('MoveAppliedEvent missing FEN field:', event)
        return
      }
      chessRef.current.load(event.fen)
      const currentTurn = chessRef.current.turn()
      setGameState((s) => ({
        ...s,
        fen: event.fen,
        currentTurn
      }))
      // Only add if this move isn't already in history (prevent duplicates)
      setMoveHistory((prev) => {
        if (prev.length > 0 && prev[prev.length - 1] === event.sanNotation) {
          return prev
        }
        return [...prev, event.sanNotation]
      })
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
    } catch (e) {
      console.error('Failed to load FEN from sync event:', e)
    }
  }

  function onPieceDrop(args: any): boolean {
    const { sourceSquare, targetSquare } = args

    if (!isMyTurn || gameState.status !== 'ACTIVE') {
      return false
    }

    if (!sourceSquare || !targetSquare) return false

    try {
      const move = chessRef.current.move({
        from: sourceSquare,
        to: targetSquare,
        promotion: 'q'
      })

      if (!move) {
        return false
      }

      setGameState((s) => ({
        ...s,
        fen: chessRef.current.fen(),
        currentTurn: chessRef.current.turn()
      }))

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
      await stompClient.send(`/app/game/${gameId}/move`, { moveUci })
    } catch (e) {
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
    <div className="page-shell game-page">
      <header className="page-header">
        <div>
          <div className="eyebrow">Game {gameId}</div>
          <h2>Head to head</h2>
          <p className="lede">Moves sync instantly. Keep this tab open while you play.</p>
        </div>
        <button className="btn ghost" onClick={() => navigate('/lobby')}>Back to lobby</button>
      </header>

      {gameState.lastError && <div className="alert error">{gameState.lastError}</div>}

      <div className="game-layout">
        <section className="board-panel">
          <div className="player-row top">
            <div>
              <div className="label">Opponent</div>
              <div className="player-name">{getOpponentInfo()}</div>
            </div>
            <div className="badge subtle">{gameState.currentTurn === 'b' ? 'To move' : 'Waiting'}</div>
          </div>

          <div className="board-shell">
            <Chessboard
              options={{
                position: gameState.fen,
                onPieceDrop,
                boardOrientation: gameState.myColor === 'black' ? 'black' : 'white'
              }}
            />
          </div>

          <div className="player-row bottom">
            <div>
              <div className="label">You</div>
              <div className="player-name">{user?.username} ({gameState.myColor?.toUpperCase()})</div>
            </div>
            {isMyTurn && <div className="badge live">Your turn</div>}
            {!isMyTurn && gameState.status === 'ACTIVE' && <div className="badge subtle">Opponent to move</div>}
          </div>
        </section>

        <aside className="info-panel">
          <div className="panel">
            <div className="panel-head">
              <div className="eyebrow">Status</div>
              <div className="badge subtle">{gameState.status}</div>
            </div>
            <div className="info-list">
              <div className="info-line">
                <span className="label">Current turn</span>
                <span>{gameState.currentTurn === 'w' ? 'White' : 'Black'}</span>
              </div>
              <div className="info-line">
                <span className="label">White ID</span>
                <span>{gameState.whitePlayerId ?? '—'}</span>
              </div>
              <div className="info-line">
                <span className="label">Black ID</span>
                <span>{gameState.blackPlayerId ?? '—'}</span>
              </div>
              {gameState.status === 'ENDED' && (
                <div className="info-line">
                  <span className="label">Result</span>
                  <span>{gameState.result} · {gameState.resultReason}</span>
                </div>
              )}
            </div>

            <div className="button-stack">
              {gameState.status === 'ACTIVE' && (
                <button className="btn ghost" onClick={handleResign}>Resign</button>
              )}
            </div>
          </div>

          <div className="panel">
            <div className="panel-head">
              <div className="eyebrow">Moves</div>
              <div className="microcopy">Move History</div>
            </div>
            {moveHistory.length === 0 ? (
              <div className="empty">No moves yet.</div>
            ) : (
              <ol className="moves-list">
                {getMovePairs().map((pair) => (
                  <li key={pair.moveNumber}>
                    {pair.white} {pair.black ? pair.black : ''}
                  </li>
                ))}
              </ol>
            )}
          </div>
        </aside>
      </div>
    </div>
  )
}


