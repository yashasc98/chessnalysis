import client, { setAuthToken } from './client'
import type { LoginResponse, RegisterResponse, LoginRequest, RegisterRequest } from '../types'

export async function login(req: LoginRequest): Promise<LoginResponse> {
  const res = await client.post('/auth/login', req)
  const data: LoginResponse = res.data
  // set default header for subsequent REST calls
  setAuthToken(data.token)
  return data
}

export async function register(req: RegisterRequest): Promise<RegisterResponse> {
  const res = await client.post('/auth/register', req)
  return res.data
}

export async function refresh(refreshToken: string) {
  const res = await client.post('/auth/refresh', { refreshToken })
  const data: LoginResponse = res.data
  setAuthToken(data.token)
  return data
}

export async function logout(deviceId: string) {
  try {
    await client.post('/auth/logout', { deviceId })
  } catch (e) {
    console.warn('Logout API call failed (may be expected if session expired):', e)
  }
}
