import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router'
import { useAuth } from './auth.jsx'
import Layout from './components/Layout.jsx'
import { Loading } from './components/ui.jsx'
import Home from './pages/Home.jsx'
import ItemDetail from './pages/ItemDetail.jsx'
import ForgotPassword from './pages/ForgotPassword.jsx'
import Login from './pages/Login.jsx'
import Profile from './pages/Profile.jsx'
import Register from './pages/Register.jsx'
import ResetPassword from './pages/ResetPassword.jsx'
import VerifyEmail from './pages/VerifyEmail.jsx'

// Pages most visitors don't open are loaded on demand (smaller first download)
const Account = lazy(() => import('./pages/Account.jsx'))
const About = lazy(() => import('./pages/About.jsx'))
const Admin = lazy(() => import('./pages/Admin.jsx'))
const NewItem = lazy(() => import('./pages/NewItem.jsx'))
const Privacy = lazy(() => import('./pages/Legal.jsx').then((m) => ({ default: m.Privacy })))
const Terms = lazy(() => import('./pages/Legal.jsx').then((m) => ({ default: m.Terms })))
const Leaderboard = lazy(() => import('./pages/Leaderboard.jsx'))
const Market = lazy(() => import('./pages/Market.jsx'))
const Compare = lazy(() => import('./pages/Compare.jsx'))
const LiveShows = lazy(() => import('./pages/LiveShows.jsx'))
const LiveShowStage = lazy(() => import('./pages/LiveShowStage.jsx'))
const NewLiveShow = lazy(() => import('./pages/NewLiveShow.jsx'))

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
    <Suspense fallback={<Loading />}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/verify-email" element={<VerifyEmail />} />
        <Route element={<Layout />}>
          {/* Publice */}
          <Route path="/" element={<Home />} />
          <Route path="/items/:id" element={<ItemDetail />} />
          <Route path="/users/:username" element={<Profile />} />
          <Route path="/despre" element={<About />} />
          <Route path="/confidentialitate" element={<Privacy />} />
          <Route path="/termeni" element={<Terms />} />
          <Route path="/clasament" element={<Leaderboard />} />
          <Route path="/piata" element={<Market />} />
          <Route path="/compara" element={<Compare />} />
          <Route path="/live" element={<LiveShows />} />
          <Route path="/live/:id" element={<LiveShowStage />} />
          <Route
            path="/live/nou"
            element={
              <RequireAuth>
                <NewLiveShow />
              </RequireAuth>
            }
          />
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
    </Suspense>
  )
}
