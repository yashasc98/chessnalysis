import { Routes, Route, Navigate } from 'react-router-dom'
import Signup from './pages/Signup'
import Login from './pages/Login'
import Lobby from './pages/Lobby'
import Game from './pages/Game'
import { useAuth } from './contexts/AuthContext'

function App() {
  const { user } = useAuth()

  return (
    <Routes>
      <Route path="/" element={user ? <Navigate to="/lobby" /> : <Navigate to="/login" />} />
      <Route path="/signup" element={<Signup />} />
      <Route path="/login" element={<Login />} />
      <Route path="/lobby" element={<Lobby />} />
      <Route path="/game/:gameId" element={<Game />} />
    </Routes>
  )
}

export default App
