import { useForm } from 'react-hook-form'
import { register as apiRegister } from '../api/auth'
import { Link, useNavigate } from 'react-router-dom'
import { useState } from 'react'

export default function Signup() {
  const { register, handleSubmit, formState } = useForm()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(data: any) {
    try {
      setError(null)
      await apiRegister({ username: data.username, password: data.password })
      navigate('/login')
    } catch (e: any) {
      setError('Registration failed: ' + (e?.response?.data || e.message))
    }
  }

  return (
    <div className="page-shell auth-page">
      <div className="auth-card">
        <div className="auth-head">
          <div className="eyebrow">Free account</div>
          <h2>Create your profile</h2>
          <p className="lede">Pick a handle, keep your games synced, and find opponents fast.</p>
        </div>

        <form className="form-grid" onSubmit={handleSubmit(onSubmit)}>
          <label className="field">
            <span>Username</span>
            <input placeholder="e.g. darksquare" {...register('username', { required: true, minLength: 3 })} />
          </label>
          <label className="field">
            <span>Password</span>
            <input type="password" placeholder="Minimum 6 characters" {...register('password', { required: true, minLength: 6 })} />
          </label>

          {error && <div className="form-error">{error}</div>}
          {formState.isSubmitting && <div className="form-hint">Creating your account...</div>}

          <button type="submit" className="btn primary full" disabled={formState.isSubmitting}>Sign up</button>
        </form>

        <div className="auth-footer">
          <span>Already playing here?</span>
          <Link to="/login">Login</Link>
        </div>
      </div>
    </div>
  )
}
