import { useEffect, useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { stompClient } from '../ws/stompClient'
import type { MatchFoundEvent, EnterQueueRequest, LeaveQueueRequest } from '../types'
import { useNavigate } from 'react-router-dom'

interface GameNavState {
  matchEvent: MatchFoundEvent
}

export default function Lobby() {
  const { user, logout } = useAuth()
  const [timeControl, setTimeControl] = useState('BLITZ_5_0')
  const [isInQueue, setIsInQueue] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    // Wait for STOMP connection before subscribing
    const subscribe = async () => {
      try {
        await stompClient.waitForConnection()
        
        // Subscribe to user-specific match-found topic
        // Note: SimpleBroker doesn't properly route /user destinations,
        // so we subscribe to /topic/user.{username}.queue.match-found instead,
        // which the backend sends to when convertAndSend is called
        if (!user?.username) {
          console.error('Username not available for subscription')
          return
        }
        
        const topicPath = `/topic/user.${user.username}.queue.match-found`
        console.log('🔴 Subscribing to match-found notifications at:', topicPath)
        
        const sub = stompClient.subscribe(topicPath, (msg) => {
          const event: MatchFoundEvent = JSON.parse(msg.body)
          console.log('🟢 Match found received:', event)
          // Navigate to game and pass opponent info via state
          const gameNavState: GameNavState = {
            matchEvent: event
          }
          navigate(`/game/${event.gameId}`, { state: gameNavState })
        })
        return () => sub && sub.unsubscribe()
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
      console.log('Attempting to enter queue with timeControl:', timeControl)
      const req: EnterQueueRequest = { timeControl }
      await stompClient.send('/app/matchmaking/enter', req)
      console.log('Successfully entered queue for', timeControl)
      setIsInQueue(true)
    } catch (e) {
      console.error('Failed to enter queue:', e)
      alert('Failed to enter queue: ' + (e as any).message)
    }
  }

  async function leaveQueue() {
    try {
      console.log('Attempting to leave queue')
      const req: LeaveQueueRequest = {}
      await stompClient.send('/app/matchmaking/leave', req)
      console.log('Successfully left queue')
      setIsInQueue(false)
    } catch (e) {
      console.error('Failed to leave queue:', e)
      alert('Failed to leave queue: ' + (e as any).message)
    }
  }

  async function handleLogout() {
    try {
      // Leave queue before logging out
      if (isInQueue) {
        await leaveQueue()
      }
      await logout()
      navigate('/login')
    } catch (e) {
      console.error('Logout error:', e)
      // Still navigate even if logout API fails
      navigate('/login')
    }
  }

  return (
    <div style={styles.container}>
      <h2>Chess Lobby</h2>
      <div style={styles.userInfo}>Logged in as: <strong>{user?.username}</strong></div>

      <div style={styles.queueSection}>
        <h3>Find Opponent</h3>
        <div style={styles.formGroup}>
          <label htmlFor="timeControl">Time Control: </label>
          <select
            id="timeControl"
            value={timeControl}
            onChange={(e) => setTimeControl(e.target.value)}
            disabled={isInQueue}
            style={styles.select}
          >
            <option value="BLITZ_5_0">5+0 Blitz</option>
            <option value="BLITZ_3_0">3+0 Blitz</option>
            <option value="RAPID_10_0">10+0 Rapid</option>
          </select>
        </div>

        <div style={styles.buttonGroup}>
          {!isInQueue ? (
            <button onClick={enterQueue} style={styles.buttonPrimary}>
              Enter Queue
            </button>
          ) : (
            <>
              <div style={styles.queueStatus}>⏳ Waiting for opponent...</div>
              <button onClick={leaveQueue} style={styles.buttonSecondary}>
                Leave Queue
              </button>
            </>
          )}
        </div>
      </div>

      <div style={styles.logoutSection}>
        <button onClick={handleLogout} style={styles.buttonLogout}>
          Logout
        </button>
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    padding: '40px 20px',
    maxWidth: '600px',
    margin: '0 auto',
    fontFamily: 'Arial, sans-serif',
    color: '#e0e0e0'
  },
  userInfo: {
    marginBottom: '30px',
    padding: '15px',
    backgroundColor: '#2a2a2a',
    borderRadius: '5px',
    fontSize: '16px',
    color: '#e0e0e0'
  },
  queueSection: {
    padding: '20px',
    backgroundColor: '#1e3a5f',
    borderRadius: '5px',
    marginBottom: '30px',
    color: '#e0e0e0'
  },
  formGroup: {
    marginBottom: '15px',
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    color: '#e0e0e0'
  },
  select: {
    padding: '8px',
    fontSize: '14px',
    borderRadius: '4px',
    border: '1px solid #555',
    backgroundColor: '#2a2a2a',
    color: '#e0e0e0'
  },
  buttonGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '10px'
  },
  buttonPrimary: {
    padding: '12px 20px',
    backgroundColor: '#1976d2',
    color: 'white',
    border: 'none',
    borderRadius: '5px',
    cursor: 'pointer',
    fontSize: '16px',
    fontWeight: 'bold'
  },
  buttonSecondary: {
    padding: '12px 20px',
    backgroundColor: '#f57c00',
    color: 'white',
    border: 'none',
    borderRadius: '5px',
    cursor: 'pointer',
    fontSize: '16px'
  },
  queueStatus: {
    padding: '12px',
    backgroundColor: '#3a3a2a',
    color: '#ffb74d',
    borderRadius: '4px',
    textAlign: 'center',
    fontSize: '16px',
    fontWeight: 'bold'
  },
  logoutSection: {
    textAlign: 'center'
  },
  buttonLogout: {
    padding: '10px 20px',
    backgroundColor: '#d32f2f',
    color: 'white',
    border: 'none',
    borderRadius: '5px',
    cursor: 'pointer',
    fontSize: '16px'
  }
}

