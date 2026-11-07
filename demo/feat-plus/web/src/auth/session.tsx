import { createContext, useContext, type ReactNode } from "react"

import type { CurrentUser } from "@/api/types"

const SessionContext = createContext<CurrentUser | null>(null)
const RefreshContext = createContext<() => void>(() => {})

/**
 * 把当前登录人下发给页面，避免每个页面都从 props 一层层传。
 * 只有登录后的布局里才有 Provider，登录页取不到是预期行为。
 *
 * onRefresh 用来重新拉一次 /api/auth/me。菜单管理页改完菜单要靠它：
 * 侧边栏的菜单是这个响应的一部分，不在 react-query 的缓存里，不重新拉就还是旧菜单。
 */
export function SessionProvider({ user, onRefresh, children }: {
  user: CurrentUser
  onRefresh?: () => void
  children: ReactNode
}) {
  return (
    <SessionContext.Provider value={user}>
      <RefreshContext.Provider value={onRefresh ?? (() => {})}>{children}</RefreshContext.Provider>
    </SessionContext.Provider>
  )
}

export function useSession(): CurrentUser {
  const user = useContext(SessionContext)
  if (!user) throw new Error("useSession 必须在登录后的布局中使用")
  return user
}

/** 没有登录态时返回 null，供需要容错的场景使用。 */
export function useOptionalSession(): CurrentUser | null {
  return useContext(SessionContext)
}

/** 重新拉取当前登录人（权限与菜单都会跟着更新）。 */
export function useRefreshSession(): () => void {
  return useContext(RefreshContext)
}
