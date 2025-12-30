import { useForm } from 'react-hook-form'
import { register as apiRegister } from '../api/auth'
import { useNavigate } from 'react-router-dom'

export default function Signup() {
  const { register, handleSubmit, formState } = useForm()
  const navigate = useNavigate()

  async function onSubmit(data: any) {
    try {
      await apiRegister({ username: data.username, password: data.password })
      navigate('/login')
    } catch (e: any) {
      alert('Registration failed: ' + e?.response?.data || e.message)
    }
  }

  return (
    <div>
      <h2>Sign up</h2>
      <form onSubmit={handleSubmit(onSubmit)}>
        <div>
          <label>Username</label>
          <input {...register('username', { required: true, minLength: 3 })} />
        </div>
        <div>
          <label>Password</label>
          <input type="password" {...register('password', { required: true, minLength: 6 })} />
        </div>
        <button type="submit">Sign up</button>
        {formState.isSubmitting && <div>Creating...</div>}
      </form>
    </div>
  )
}
