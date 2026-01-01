import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const timeControlPresets = [
  { code: 'BULLET_1_0', label: '1 + 0', name: 'Bullet', detail: 'Pure instinct and flag battles.' },
  { code: 'BLITZ_3_0', label: '3 + 0', name: 'Blitz', detail: 'Classic internet blitz pace.' },
  { code: 'BLITZ_5_3', label: '5 + 3', name: 'Blitz', detail: 'Room to calculate with a soft increment.' },
  { code: 'RAPID_10_0', label: '10 + 0', name: 'Rapid', detail: 'Settle in and build plans.' },
  { code: 'RAPID_15_10', label: '15 + 10', name: 'Rapid', detail: 'Tournament-style focus sessions.' }
]

export default function Landing() {
  const { user } = useAuth()
  const navigate = useNavigate()

  const primaryCta = () => {
    if (user) return navigate('/lobby')
    navigate('/login')
  }

  return (
    <div className="page-shell">
      <header className="top-nav">
        <div className="brand">
          <span className="brand-mark">♞</span>
          <span className="brand-name">ChessNalysis</span>
        </div>
        <nav className="nav-links">
          <Link to="/login">Login</Link>
          <Link to="/signup" className="pill">Create Account</Link>
          {user && <Link to="/lobby">Go to Lobby</Link>}
        </nav>
      </header>

      <main className="hero-grid">
        <section className="hero">
          <div className="eyebrow">Engine-backed, real-time chess</div>
          <h1>Play bold. Learn fast. Analyze deeper.</h1>
          <p className="lede">
            ChessNalysis brings tournament-grade play with live clocks, crisp visuals, and instant analysis-ready data. Built for players who love the feel of a dark, focused board.
          </p>
          <div className="hero-actions">
            <button className="btn primary" onClick={primaryCta}>{user ? 'Enter Lobby' : 'Login to play'}</button>
            <Link to="/signup" className="btn ghost">New here? Register</Link>
          </div>
          <div className="meta-row">
            <span>Live matchmaking</span>
            <span>Zero ads</span>
            <span>Secure accounts</span>
          </div>
        </section>

        <section className="panel spotlight">
          <h3>Choose your rhythm</h3>
          <div className="time-card-grid">
            {timeControlPresets.map((preset) => (
              <div className="time-card" key={preset.code}>
                <div className="time-label">{preset.label}</div>
                <div className="time-name">{preset.name}</div>
                <div className="time-detail">{preset.detail}</div>
              </div>
            ))}
          </div>
          {/* <p className="microcopy">Inspired by lichess-style speed panels — pick, click, and play.</p> */}
        </section>
      </main>

      <section className="feature-grid">
        <div className="feature-card">
          <div className="feature-badge">Matchmaking</div>
          <h4>Get paired instantly</h4>
          <p>Queue up, lock in your time control, and get routed to opponents without lobby noise.</p>
        </div>
        <div className="feature-card">
          <div className="feature-badge">Live clocks</div>
          <h4>Server-tracked timers</h4>
          <p>Authoritative clocks with increments and timeout detection keep games fair and synchronized.</p>
        </div>
        <div className="feature-card">
          <div className="feature-badge">Clean visuals</div>
          <h4>Dark, tournament feel</h4>
          <p>Modern palette, subtle depth, and typography tuned for focus.</p>
        </div>
        <div className="feature-card">
          <div className="feature-badge">Analysis ready</div>
          <h4>Take the game with you</h4>
          <p>Every move and FEN is preserved so you can review lines right after you flag or mate.</p>
        </div>
      </section>

      <section className="cta-panel">
        <div>
          <h3>Ready when you are</h3>
          <p>Log in to jump into the lobby or create a free account to start building your record.</p>
        </div>
        <div className="cta-actions">
          <Link to="/login" className="btn primary">Login</Link>
          <Link to="/signup" className="btn ghost">Register</Link>
        </div>
      </section>
    </div>
  )
}
