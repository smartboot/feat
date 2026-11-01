import type {
  CurrentUser,
  DashboardSummary,
  LoginResponse,
  NotificationItem,
  PageResult,
  UserInput,
  UserListItem,
  UserQuery,
  UserStatus,
} from "@/api/types"
import { request } from "@/api/client"
import type { AdminService } from "@/services/admin-service"

function buildQuery(query: UserQuery): string {
  const params = new URLSearchParams()
  params.set("page", String(query.page))
  params.set("pageSize", String(query.pageSize))
  if (query.keyword.trim()) params.set("keyword", query.keyword.trim())
  if (query.status !== "all") params.set("status", query.status)
  if (query.roleCode !== "all") params.set("roleCode", query.roleCode)
  if (query.department.trim()) params.set("department", query.department.trim())
  return params.toString()
}

const jsonBody = (payload: unknown) => JSON.stringify(payload)

/**
 * 真实后端实现，对应 feat-plus 的 Feat Cloud 接口。
 * 与 mock-admin-service 实现同一个 AdminService 契约，页面代码不需要改动。
 */
export const adminService: AdminService = {
  login(username: string, password: string): Promise<LoginResponse> {
    return request<LoginResponse>("/api/auth/login", { method: "POST", body: jsonBody({ username, password }) })
  },
  currentUser(): Promise<CurrentUser> {
    return request<CurrentUser>("/api/auth/me")
  },
  listUsers(query: UserQuery): Promise<PageResult<UserListItem>> {
    return request<PageResult<UserListItem>>(`/api/users?${buildQuery(query)}`)
  },
  getAllUsers(): Promise<UserListItem[]> {
    return request<UserListItem[]>("/api/users/all")
  },
  getUser(id: number): Promise<UserListItem> {
    return request<UserListItem>(`/api/users/${id}`)
  },
  createUser(input: UserInput): Promise<UserListItem> {
    return request<UserListItem>("/api/users", { method: "POST", body: jsonBody(input) })
  },
  updateUser(id: number, input: UserInput): Promise<UserListItem> {
    return request<UserListItem>(`/api/users/${id}`, { method: "PUT", body: jsonBody(input) })
  },
  setUserStatus(id: number, status: UserStatus): Promise<UserListItem> {
    return request<UserListItem>(`/api/users/${id}/status`, { method: "POST", body: jsonBody({ status }) })
  },
  resetUserPassword(id: number): Promise<{ temporaryPassword: string }> {
    return request<{ temporaryPassword: string }>(`/api/users/${id}/reset-password`, { method: "POST" })
  },
  deleteUser(id: number): Promise<void> {
    return request<void>(`/api/users/${id}`, { method: "DELETE" })
  },
  getDashboardSummary(): Promise<DashboardSummary> {
    return request<DashboardSummary>("/api/dashboard")
  },
  listNotifications(): Promise<NotificationItem[]> {
    return request<NotificationItem[]>("/api/notifications")
  },
  markNotificationRead(id: number): Promise<void> {
    return request<void>(`/api/notifications/${id}/read`, { method: "POST" })
  },
  markAllNotificationsRead(): Promise<void> {
    return request<void>("/api/notifications/read-all", { method: "POST" })
  },
  resetDemo(): Promise<void> {
    return request<void>("/api/admin/reset-demo", { method: "POST" })
  },
}
