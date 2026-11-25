import { useCallback, createContext, useContext, useEffect, useState } from 'react'
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

  // Returns { twoFactorTicket } when the account has 2FA: then call loginTwoFactor with the code
  async function login(username, password) {
    const result = await api('/auth/login', {
      method: 'POST',
      body: { username, password },
      auth: false,
    })
    if (result.twoFactorRequired) return { twoFactorTicket: result.twoFactorTicket }
    setToken(result.token)
    setUser(await api('/users/me'))
    return {}
  }

  async function loginTwoFactor(ticket, code) {
    const { token } = await api('/auth/login/2fa', { method: 'POST', body: { ticket, code }, auth: false })
    setToken(token)
    setUser(await api('/users/me'))
  }

  // Returns the server answer; when the email must be confirmed first, nobody is logged in yet
  async function register(username, email, password) {
    const result = await api('/auth/register', {
      method: 'POST',
      body: { username, email, password },
      auth: false,
    })
    if (!result?.verificationRequired) await login(username, password)
    return result
  }

  const loginWithGoogle = useCallback(async (credential) => {
    const { token } = await api('/auth/google', { method: 'POST', body: { credential }, auth: false })
    setToken(token)
    setUser(await api('/users/me'))
  }, [])

  function logout() {
    clearToken()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, loginTwoFactor, register, logout, loginWithGoogle }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
