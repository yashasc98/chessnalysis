import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Chess } from 'chess.js'
import { Chessboard } from 'react-chessboard'
import { stompClient } from '../ws/stompClient'
import type { MoveAppliedEvent, GameStateSyncEvent, IllegalMoveEvent, GameStartedEvent, GameEndedEvent } from '../types'

export default function Game() {
  const { gameId } = useParams()
  const chessRef = useRef(new Chess())
  const [fen, setFen] = useState(chessRef.current.fen())
  const [turn, setTurn] = useState<'w' | 'b'>(chessRef.current.turn())

  useEffect(() => {
    if (!gameId) return

    const setupSubscriptions = async () => {
      try {
        await stompClient.waitForConnection()

        const topic = `/topic/game.${gameId}`
        const subs = [] as any[]

        subs.push(
          stompClient.subscribe(topic, (msg) => {
            const payload = JSON.parse(msg.body)
            handleEvent(payload)
          })
        )

        // Also subscribe to user queue game-sync notifications
        const syncSub = stompClient.subscribe('/user/queue/game-sync', (msg) => {
          const payload: GameStateSyncEvent = JSON.parse(msg.body)
          if (payload.gameId === gameId) applySync(payload)
        })
        subs.push(syncSub)

        // Request sync in case we're reconnecting
        stompClient.send(`/app/game/${gameId}/sync`, {})

        return () => subs.forEach((s) => s.unsubscribe && s.unsubscribe())
      } catch (e) {
        console.error('Failed to setup game subscriptions:', e)
      }
    }

    let cleanup: (() => void) | undefined
    setupSubscriptions().then((fn) => {
      cleanup = fn
    })

    return () => cleanup?.()
  }, [gameId])

  function handleEvent(payload: any) {
    if ((payload as MoveAppliedEvent).moveUci) applyMoveEvent(payload as MoveAppliedEvent)
    else if ((payload as IllegalMoveEvent).reason) alert('Illegal move: ' + (payload as IllegalMoveEvent).reason)
    else if ((payload as GameStartedEvent).whitePlayerId) console.log('Game started', payload)
    else if ((payload as GameEndedEvent).result) console.log('Game ended', payload)
  }

  function applyMoveEvent(ev: MoveAppliedEvent) {
    try {
      chessRef.current.load(ev.fen)
    } catch (e) {
      console.error('Failed to load FEN', e)
    }
    setFen(ev.fen)
    setTurn(chessRef.current.turn())
  }

  function applySync(ev: GameStateSyncEvent) {
    chessRef.current.load(ev.fen)
    setFen(ev.fen)
    setTurn(chessRef.current.turn())
  }

  function onPieceDrop(args: any): boolean {
    const { sourceSquare, targetSquare } = args
    if (!sourceSquare || !targetSquare) return false
    
    const move = chessRef.current.move({ from: sourceSquare, to: targetSquare, promotion: 'q' })
    if (!move) {
      // illegal locally
      alert('Illegal move')
      return false
    }

    setFen(chessRef.current.fen())
    // send UCI via WS
    const uci = sourceSquare + targetSquare + (move.promotion ? move.promotion : '')
    stompClient.send(`/app/game/${gameId}/move`, { moveUci: uci })
    return true
  }

  return (
    <div>
      <h2>Game {gameId}</h2>
      <div>Turn: {turn}</div>
      <div style={{ width: 500, height: 500 }}>
        <Chessboard options={{ position: fen, onPieceDrop }} />
      </div>
    </div>
  )
}
