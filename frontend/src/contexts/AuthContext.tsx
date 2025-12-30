import { createContext, useContext, useEffect, useState } from 'react'
import type { LoginResponse } from '../types'
import { login as apiLogin, logout as apiLogout } from '../api/auth'
import { stompClient } from '../ws/stompClient'
import { setAuthToken } from '../api/client'

type User = { userId: number; username: string; role: string }

type AuthContextType = {
  user: User | null
  token: string | null
  deviceId: string | null
  login: (req: { username: string; password: string }) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

function readTokenFromStorage() {
  try {
    const raw = localStorage.getItem('auth')
    if (!raw) return null
    return JSON.parse(raw) as LoginResponse
  } catch {
    return null
  }
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => readTokenFromStorage()?.token ?? null)
  const [deviceId, setDeviceId] = useState<string | null>(() => readTokenFromStorage()?.deviceId ?? null)
  const [user, setUser] = useState<User | null>(() => {
    const t = readTokenFromStorage()
    if (!t) return null
    return { userId: t.userId, username: t.username, role: t.role }
  })

  useEffect(() => {
    if (token) {
      setAuthToken(token)
      stompClient.connect(token).catch((e) => console.error('Failed to connect STOMP:', e))
    }
  }, [token])

  const doLogin = async (req: { username: string; password: string }) => {
    const res = await apiLogin({ username: req.username, password: req.password })
    setToken(res.token)
    setDeviceId(res.deviceId)
    setUser({ userId: res.userId, username: res.username, role: res.role })
    try {
      localStorage.setItem('auth', JSON.stringify(res))
    } catch {
      // ignore
    }
  }

  const doLogout = async () => {
    // Call logout API to invalidate tokens on backend
    if (deviceId) {
      await apiLogout(deviceId)
    }
    // Clear local state
    setToken(null)
    setDeviceId(null)
    setUser(null)
    setAuthToken(undefined)
    stompClient.disconnect()
    try {
      localStorage.removeItem('auth')
    } catch {}
  }

  return <AuthContext.Provider value={{ user, token, deviceId, login: doLogin, logout: doLogout }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
