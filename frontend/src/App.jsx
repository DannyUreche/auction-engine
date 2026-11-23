import { Navigate, Route, Routes, useLocation } from 'react-router'
import { useAuth } from './auth.jsx'
import Layout from './components/Layout.jsx'
import { Loading } from './components/ui.jsx'
import Account from './pages/Account.jsx'
import Admin from './pages/Admin.jsx'
import Home from './pages/Home.jsx'
import ItemDetail from './pages/ItemDetail.jsx'
import Login from './pages/Login.jsx'
import NewItem from './pages/NewItem.jsx'
import Profile from './pages/Profile.jsx'
import Register from './pages/Register.jsx'

// Paginile pentru vanzare si cont cer autentificare; licitatiile se pot vedea si fara cont
// adminOnly: pagina e doar pentru administratori (ceilalti sunt trimisi pe pagina principala)
function RequireAuth({ children, adminOnly = false }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) return <Loading />
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (adminOnly && !user.admin) return <Navigate to="/" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route element={<Layout />}>
        {/* Publice */}
        <Route path="/" element={<Home />} />
        <Route path="/items/:id" element={<ItemDetail />} />
        <Route path="/users/:username" element={<Profile />} />
        {/* Doar pentru utilizatori logati */}
        <Route
          path="/sell"
          element={
            <RequireAuth>
              <NewItem />
            </RequireAuth>
          }
        />
        <Route
          path="/account"
          element={
            <RequireAuth>
              <Account />
            </RequireAuth>
          }
        />
        <Route
          path="/admin"
          element={
            <RequireAuth adminOnly>
              <Admin />
            </RequireAuth>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
