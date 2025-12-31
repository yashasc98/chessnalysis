import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useState } from 'react'

export default function Login() {
  const { register, handleSubmit } = useForm()
  const auth = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(data: any) {
    try {
      setError(null)
      await auth.login({ username: data.username, password: data.password })
      navigate('/lobby')
    } catch (e: any) {
      setError('Login failed: ' + (e?.response?.data || e.message))
    }
  }

  return (
    <div className="page-shell auth-page">
      <div className="auth-card">
        <div className="auth-head">
          <div className="eyebrow">Welcome back</div>
          <h2>Login to ChessNalysis</h2>
          <p className="lede">Stay synced across devices with secure, server-tracked games.</p>
        </div>

        <form className="form-grid" onSubmit={handleSubmit(onSubmit)}>
          <label className="field">
            <span>Username</span>
            <input placeholder="e.g. knightfox" {...register('username', { required: true })} />
          </label>
          <label className="field">
            <span>Password</span>
            <input type="password" placeholder="••••••••" {...register('password', { required: true })} />
          </label>

          {error && <div className="form-error">{error}</div>}

          <button type="submit" className="btn primary full">Login</button>
        </form>

        <div className="auth-footer">
          <span>New to ChessNalysis?</span>
          <Link to="/signup">Create an account</Link>
        </div>
      </div>
    </div>
  )
}
