import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { stompClient } from '../ws/stompClient'
import type { MatchFoundEvent, EnterQueueRequest, LeaveQueueRequest } from '../types'

interface GameNavState {
  matchEvent: MatchFoundEvent
}

const timeCardOptions = [
  { value: 'BULLET_1_0', label: '1 + 0', name: 'Bullet', detail: 'Instinct and speed' },
  { value: 'BLITZ_3_0', label: '3 + 0', name: 'Blitz', detail: 'Classic internet blitz' },
  { value: 'BLITZ_5_0', label: '5 + 0', name: 'Blitz', detail: 'More time to calculate' },
  { value: 'BLITZ_5_3', label: '5 + 3', name: 'Blitz', detail: 'Smooth increment feel' },
  { value: 'RAPID_10_0', label: '10 + 0', name: 'Rapid', detail: 'Structured play' }
]

export default function Lobby() {
  const { user, logout } = useAuth()
  const [timeControl, setTimeControl] = useState('BLITZ_5_0')
  const [isInQueue, setIsInQueue] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    const subscribe = async () => {
      try {
        await stompClient.waitForConnection()

        if (!user?.username) {
          console.error('Username not available for subscription')
          return
        }

        const topicPath = `/topic/user.${user.username}.queue.match-found`
        const sub = stompClient.subscribe(topicPath, (msg) => {
          const event: MatchFoundEvent = JSON.parse(msg.body)
          const gameNavState: GameNavState = { matchEvent: event }
          navigate(`/game/${event.gameId}`, { state: gameNavState })
        })
        return () => sub?.unsubscribe()
      } catch (e) {
        console.error('Failed to subscribe:', e)
      }
    }

    let unsubscribe: (() => void) | undefined
    subscribe().then((fn) => {
      unsubscribe = fn
    })

    return () => unsubscribe?.()
  }, [navigate, user?.username])

  async function enterQueue() {
    try {
      const req: EnterQueueRequest = { timeControl }
      await stompClient.send('/app/matchmaking/enter', req)
      setIsInQueue(true)
    } catch (e) {
      console.error('Failed to enter queue:', e)
      alert('Failed to enter queue: ' + (e as any).message)
    }
  }

  async function leaveQueue() {
    try {
      const req: LeaveQueueRequest = {}
      await stompClient.send('/app/matchmaking/leave', req)
      setIsInQueue(false)
    } catch (e) {
      console.error('Failed to leave queue:', e)
      alert('Failed to leave queue: ' + (e as any).message)
    }
  }

  async function handleLogout() {
    try {
      if (isInQueue) {
        await leaveQueue()
      }
      await logout()
      navigate('/login')
    } catch (e) {
      console.error('Logout error:', e)
      navigate('/login')
    }
  }

  return (
    <div className="page-shell lobby-page">
      <header className="page-header">
        <div>
          <div className="eyebrow">Lobby</div>
          <h2>Ready to play, {user?.username}</h2>
          <p className="lede">Pick a time control card and we will pair you automatically.</p>
        </div>
        <button className="btn ghost" onClick={handleLogout}>Logout</button>
      </header>

      <section className="panel">
        <div className="panel-head">
          <div>
            <div className="eyebrow">Time controls</div>
            <h3>Choose your pace</h3>
          </div>
          {isInQueue && <div className="badge subtle">Queued</div>}
        </div>

        <div className="time-card-grid selectable">
          {timeCardOptions.map((opt) => (
            <button
              key={opt.value}
              className={`time-card selectable ${timeControl === opt.value ? 'selected' : ''}`}
              onClick={() => setTimeControl(opt.value)}
              disabled={isInQueue}
            >
              <div className="time-label">{opt.label}</div>
              <div className="time-name">{opt.name}</div>
              <div className="time-detail">{opt.detail}</div>
            </button>
          ))}
        </div>

        <div className="queue-actions">
          {!isInQueue ? (
            <button className="btn primary" onClick={enterQueue}>Enter queue</button>
          ) : (
            <div className="queue-status">
              <div className="pulse" />
              <div>
                <div className="status-label">Searching for opponent</div>
                <div className="status-sub">Stay on this page — we will move you when found.</div>
              </div>
              <button className="btn ghost" onClick={leaveQueue}>Leave queue</button>
            </div>
          )}
        </div>
      </section>
    </div>
  )
}

