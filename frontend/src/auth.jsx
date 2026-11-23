import { createContext, useContext, useEffect, useState } from 'react'
import { api, clearToken, getToken, setToken } from './api.js'

const AuthContext = createContext(null)

// Tine minte utilizatorul logat si ofera login / register / logout oriunde in aplicatie
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(Boolean(getToken()))

  // La pornire: daca avem un token salvat, verificam daca mai e valid
  useEffect(() => {
    if (!getToken()) return
    api('/users/me')
      .then(setUser)
      .catch(() => clearToken())
      .finally(() => setLoading(false))
  }, [])

  // Cand backend-ul raspunde cu 401, deconectam utilizatorul
  useEffect(() => {
    const handleExpired = () => {
      clearToken()
      setUser(null)
    }
    window.addEventListener('auth:expired', handleExpired)
    return () => window.removeEventListener('auth:expired', handleExpired)
  }, [])

  async function login(username, password) {
    const { token } = await api('/auth/login', {
      method: 'POST',
      body: { username, password },
      auth: false,
    })
    setToken(token)
    setUser(await api('/users/me'))
  }

  async function register(username, email, password) {
    await api('/auth/register', {
      method: 'POST',
      body: { username, email, password },
      auth: false,
    })
    await login(username, password)
  }

  function logout() {
    clearToken()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
