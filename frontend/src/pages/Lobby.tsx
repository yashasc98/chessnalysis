import { useEffect, useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { stompClient } from '../ws/stompClient'
import type { MatchFoundEvent, EnterQueueRequest, LeaveQueueRequest } from '../types'
import { useNavigate } from 'react-router-dom'

export default function Lobby() {
  const { user, logout } = useAuth()
  const [timeControl, setTimeControl] = useState('BLITZ_5_0')
  const navigate = useNavigate()

  useEffect(() => {
    // Wait for STOMP connection before subscribing
    const subscribe = async () => {
      try {
        await stompClient.waitForConnection()
        // Subscribe to user-specific match-found queue
        const sub = stompClient.subscribe('/user/queue/match-found', (msg) => {
          const event: MatchFoundEvent = JSON.parse(msg.body)
          console.log('Match found', event)
          navigate(`/game/${event.gameId}`)
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
  }, [navigate])

  async function enterQueue() {
    try {
      console.log('Attempting to enter queue with timeControl:', timeControl)
      const req: EnterQueueRequest = { timeControl }
      await stompClient.send('/app/matchmaking/enter', req)
      console.log('Successfully entered queue for', timeControl)
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
    } catch (e) {
      console.error('Failed to leave queue:', e)
      alert('Failed to leave queue: ' + (e as any).message)
    }
  }

  async function handleLogout() {
    try {
      await logout()
      navigate('/login')
    } catch (e) {
      console.error('Logout error:', e)
      // Still navigate even if logout API fails
      navigate('/login')
    }
  }

  return (
    <div>
      <h2>Lobby</h2>
      <div>Logged in as: {user?.username}</div>
      <div>
        <label>Time control</label>
        <select value={timeControl} onChange={(e) => setTimeControl(e.target.value)}>
          <option value="BLITZ_5_0">5+0 Blitz</option>
          <option value="BLITZ_3_0">3+0 Blitz</option>
          <option value="RAPID_10_0">10+0 Rapid</option>
        </select>
      </div>
      <div>
        <button onClick={enterQueue}>Enter Queue</button>
        <button onClick={leaveQueue}>Leave Queue</button>
      </div>
      <div>
        <button onClick={handleLogout}>Logout</button>
      </div>
    </div>
  )
}
