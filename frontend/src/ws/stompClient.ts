import { Client } from '@stomp/stompjs'

/**
 * STOMP client wrapper for WebSocket connection.
 * 
 * The WebSocket connects automatically after login and keeps trying to reconnect
 * if connection is lost. This is expected behavior - it maintains the connection
 * for receiving match-found notifications in /user/queue/match-found.
 * 
 * You do NOT need to manually manage the connection lifecycle - it happens
 * automatically via AuthContext when user logs in/out.
 */
class StompClient {
  private client: Client | null = null
  private token: string | null = null
  private readyPromise: Promise<void> | null = null
  private readyResolve: (() => void) | null = null

  connect(token: string): Promise<void> {
    if (this.client && this.client.connected) {
      return Promise.resolve()
    }

    // Return a promise that resolves when connection is ready
    this.readyPromise = new Promise<void>((resolve) => {
      this.readyResolve = resolve
    })

    this.token = token
    this.client = new Client({
      brokerURL: (import.meta.env.VITE_WS_URL as string) || 'ws://localhost:8080/ws/game',
      connectHeaders: { Authorization: `Bearer ${token}` },
      debug: (msg) => console.debug('[STOMP]', msg),
      reconnectDelay: 5000,
      heartbeatIncoming: 25000,
      heartbeatOutgoing: 25000,
      onConnect: () => {
        console.log('STOMP connected')
        this.readyResolve?.()
      },
    })

    this.client.onStompError = (frame) => {
      console.error('Broker reported error: ', frame)
    }

    this.client.activate()
    return this.readyPromise
  }

  waitForConnection(): Promise<void> {
    if (this.client && this.client.connected) {
      return Promise.resolve()
    }
    if (this.readyPromise) {
      return this.readyPromise
    }
    return Promise.reject(new Error('STOMP client not initialized'))
  }

  disconnect() {
    if (!this.client) return
    this.client.deactivate()
    this.client = null
    this.token = null
  }

  subscribe(destination: string, callback: (msg: any) => void) {
    if (!this.client) throw new Error('STOMP client not initialized')
    return this.client.subscribe(destination, callback)
  }

  async send(destination: string, body: any) {
    await this.waitForConnection()
    if (!this.client) throw new Error('STOMP client not initialized')
    const headers: any = {}
    if (this.token) headers['Authorization'] = `Bearer ${this.token}`
    this.client.publish({ destination, body: JSON.stringify(body), headers })
  }
}

export const stompClient = new StompClient()
