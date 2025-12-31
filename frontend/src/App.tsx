import { Routes, Route, Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import './App.css'
import Signup from './pages/Signup'
import Login from './pages/Login'
import Lobby from './pages/Lobby'
import Game from './pages/Game'
import Landing from './pages/Landing.tsx'
import { useAuth } from './contexts/AuthContext'

function RequireAuth({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  if (!user) {
    return <Navigate to="/login" replace />
  }
  return children
}

function App() {
  const { user } = useAuth()

  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/signup" element={user ? <Navigate to="/lobby" replace /> : <Signup />} />
      <Route path="/login" element={user ? <Navigate to="/lobby" replace /> : <Login />} />
      <Route
        path="/lobby"
        element={
          <RequireAuth>
            <Lobby />
          </RequireAuth>
        }
      />
      <Route
        path="/game/:gameId"
        element={
          <RequireAuth>
            <Game />
          </RequireAuth>
        }
      />
    </Routes>
  )
}

export default App
