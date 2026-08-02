import { useEffect, useState } from "react"
import { Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom"

import type { CurrentUser } from "@/api/types"
import { AppLayout } from "@/app/AppLayout"
import { clearToken, getToken, setToken } from "@/auth/token"
import { adminService } from "@/services/mock-admin-service"
import { DashboardPage } from "@/pages/DashboardPage"
import { LoginPage } from "@/pages/LoginPage"
import { UserListPage } from "@/pages/UserListPage"

function ProtectedLayout({ currentUser, onLogout }: { currentUser: CurrentUser; onLogout: () => void }) {
  return <AppLayout currentUser={currentUser} onLogout={onLogout} />
}

function AuthenticatedApp({ currentUser, onLogout }: { currentUser: CurrentUser; onLogout: () => void }) {
  return (
    <Routes>
      <Route element={<ProtectedLayout currentUser={currentUser} onLogout={onLogout} />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/users" element={<UserListPage />} />
        <Route path="/users/:userId" element={<UserListPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}

function LoginRoute({ onLogin }: { onLogin: (token: string, user: CurrentUser) => void }) {
  const navigate = useNavigate()
  const location = useLocation()
  return <LoginPage onSuccess={(token, user) => { onLogin(token, user); navigate((location.state as { from?: string } | null)?.from || "/dashboard", { replace: true }) }} />
}

export default function App() {
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null)
  const [restoring, setRestoring] = useState(Boolean(getToken()))
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    if (!getToken()) {
      setRestoring(false)
      return
    }
    adminService.currentUser().then(setCurrentUser).catch(() => clearToken()).finally(() => setRestoring(false))
  }, [])

  function handleLogin(token: string, user: CurrentUser) {
    setToken(token)
    setCurrentUser(user)
  }

  function handleLogout() {
    clearToken()
    setCurrentUser(null)
    navigate("/login", { replace: true })
  }

  if (restoring) return <div className="flex min-h-screen flex-col items-center justify-center gap-3"><div className="size-10 animate-pulse rounded-xl bg-primary shadow-lg shadow-primary/20" /><strong>Feat Plus</strong><p className="text-sm text-muted-foreground">正在恢复工作空间...</p></div>
  if (!currentUser) return <Routes><Route path="/login" element={<LoginRoute onLogin={handleLogin} />} /><Route path="*" element={<Navigate to="/login" replace state={{ from: location.pathname }} />} /></Routes>
  return <AuthenticatedApp currentUser={currentUser} onLogout={handleLogout} />
}
