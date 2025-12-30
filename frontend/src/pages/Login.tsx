import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function Login() {
  const { register, handleSubmit } = useForm()
  const auth = useAuth()
  const navigate = useNavigate()

  async function onSubmit(data: any) {
    try {
      await auth.login({ username: data.username, password: data.password })
      navigate('/lobby')
    } catch (e: any) {
      alert('Login failed: ' + e?.response?.data || e.message)
    }
  }

  return (
    <div>
      <h2>Login</h2>
      <form onSubmit={handleSubmit(onSubmit)}>
        <div>
          <label>Username</label>
          <input {...register('username', { required: true })} />
        </div>
        <div>
          <label>Password</label>
          <input type="password" {...register('password', { required: true })} />
        </div>
        <button type="submit">Login</button>
      </form>
    </div>
  )
}
