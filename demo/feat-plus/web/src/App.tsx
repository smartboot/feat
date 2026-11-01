import { useEffect, useState } from "react"
import { Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom"

import type { CurrentUser } from "@/api/types"
import { setUnauthorizedHandler } from "@/api/client"
import { AppLayout } from "@/app/AppLayout"
import { clearToken, getToken, setToken } from "@/auth/token"
import { adminService } from "@/services/http-admin-service"
import { ActivityLogPage } from "@/pages/ActivityLogPage"
import { DashboardPage } from "@/pages/DashboardPage"
import { DepartmentPage } from "@/pages/DepartmentPage"
import { LoginPage } from "@/pages/LoginPage"
import { ProfilePage } from "@/pages/ProfilePage"
import { RoleListPage } from "@/pages/RoleListPage"
import { UserListPage } from "@/pages/UserListPage"

function ProtectedLayout({ currentUser, onLogout }: { currentUser: CurrentUser; onLogout: () => void }) {
  return <AppLayout currentUser={currentUser} onLogout={onLogout} />
}

function AuthenticatedApp({ currentUser, onLogout, onRefreshUser }: { currentUser: CurrentUser; onLogout: () => void; onRefreshUser: () => void }) {
  return (
    <Routes>
      <Route element={<ProtectedLayout currentUser={currentUser} onLogout={onLogout} />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/users" element={<UserListPage />} />
        <Route path="/users/:userId" element={<UserListPage />} />
        <Route path="/roles" element={<RoleListPage />} />
        <Route path="/departments" element={<DepartmentPage />} />
        <Route path="/activities" element={<ActivityLogPage />} />
        <Route path="/profile" element={<ProfilePage onProfileUpdated={onRefreshUser} />} />
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
    setUnauthorizedHandler(() => setCurrentUser(null))
    return () => setUnauthorizedHandler(null)
  }, [])

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

  /**
   * 改完自己的资料后重新拉一次 me：侧边栏的名字与头像首字母都来自它，
   * 不刷新的话页面上会出现「个人中心显示新名字、左边还是旧名字」。
   */
  function handleRefreshUser() {
    adminService.currentUser().then(setCurrentUser).catch(() => {})
  }

  function handleLogout() {
    clearToken()
    setCurrentUser(null)
    navigate("/login", { replace: true })
  }

  if (restoring) return <div className="flex min-h-screen flex-col items-center justify-center gap-3"><div className="size-10 animate-pulse rounded-xl bg-primary shadow-lg shadow-primary/20" /><strong>Feat Plus</strong><p className="text-sm text-muted-foreground">正在恢复工作空间...</p></div>
  if (!currentUser) return <Routes><Route path="/login" element={<LoginRoute onLogin={handleLogin} />} /><Route path="*" element={<Navigate to="/login" replace state={{ from: location.pathname }} />} /></Routes>
  return <AuthenticatedApp currentUser={currentUser} onLogout={handleLogout} onRefreshUser={handleRefreshUser} />
}
