import { createContext, useContext, type ReactNode } from "react"

import type { CurrentUser } from "@/api/types"

const SessionContext = createContext<CurrentUser | null>(null)

/**
 * 把当前登录人下发给页面，避免每个页面都从 props 一层层传。
 * 只有登录后的布局里才有 Provider，登录页取不到是预期行为。
 */
export function SessionProvider({ user, children }: { user: CurrentUser; children: ReactNode }) {
  return <SessionContext.Provider value={user}>{children}</SessionContext.Provider>
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
