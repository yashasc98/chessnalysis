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
  private readyReject: ((error: Error) => void) | null = null

  connect(token: string): Promise<void> {
    if (this.client && this.client.connected) {
      return Promise.resolve()
    }

    // Return a promise that resolves when connection is ready
    this.readyPromise = new Promise<void>((resolve, reject) => {
      this.readyResolve = resolve
      this.readyReject = reject
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
        // Small delay to ensure client is fully ready for subscriptions
        // Some internal state needs to stabilize after connection callback fires
        setTimeout(() => {
          this.readyResolve?.()
        }, 50)
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
      console.log('[StompClient] Already connected')
      return Promise.resolve()
    }
    // If we have a ready promise (connection in progress), wait for it
    if (this.readyPromise) {
      console.log('[StompClient] Waiting for existing connection promise')
      return this.readyPromise
    }
    // If neither, create a timeout to prevent hanging forever
    // The connection will be initiated by AuthContext's useEffect
    console.log('[StompClient] No connection initiated yet, waiting for connect() to be called')
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error('STOMP connection timeout - no connection initiated within 10s'))
      }, 10000)
      
      // Check every 100ms if a connection has been initiated
      const checkInterval = setInterval(() => {
        if (this.readyPromise) {
          console.log('[StompClient] Connection promise detected, waiting for it')
          clearTimeout(timeout)
          clearInterval(checkInterval)
          this.readyPromise!.then(resolve, reject)
        } else if (this.client && this.client.connected) {
          console.log('[StompClient] Connection already established')
          clearTimeout(timeout)
          clearInterval(checkInterval)
          resolve()
        }
      }, 100)
    })
  }

  disconnect() {
    if (!this.client) return
    this.client.deactivate()
    this.client = null
    this.token = null
  }

  subscribe(destination: string, callback: (msg: any) => void) {
    if (!this.client) {
      throw new Error('STOMP client not initialized')
    }
    
    try {
      return this.client.subscribe(destination, callback)
    } catch (e: any) {
      // If subscription fails due to connection not being ready, retry after a brief delay
      if (e.message && e.message.includes('no underlying STOMP connection')) {
        console.warn('[StompClient] Connection not ready for subscription, retrying in 100ms...')
        throw new Error(`Failed to subscribe immediately, please retry: ${e.message}`)
      }
      throw e
    }
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
