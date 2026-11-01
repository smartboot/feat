import type {
  ActivityItem,
  ActivityQuery,
  CurrentUser,
  DashboardSummary,
  DepartmentInput,
  DepartmentNode,
  LoginResponse,
  NotificationItem,
  OperatorOption,
  PageResult,
  RoleInput,
  RoleItem,
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
  if (query.departmentId !== "all") params.set("departmentId", String(query.departmentId))
  return params.toString()
}

function buildActivityQuery(query: ActivityQuery): string {
  const params = new URLSearchParams()
  params.set("page", String(query.page))
  params.set("pageSize", String(query.pageSize))
  if (query.kind !== "all") params.set("kind", query.kind)
  if (query.operatorId !== "all") params.set("operatorId", String(query.operatorId))
  if (query.from) params.set("from", query.from)
  if (query.to) params.set("to", query.to)
  return params.toString()
}

const jsonBody = (payload: unknown) => JSON.stringify(payload)

/**
 * 真实后端实现，对应 feat-plus 的 Feat Cloud 接口。
 * 页面代码一律从这里取 adminService——不要在页面里引用任何 mock 实现。
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
  listRoles(): Promise<RoleItem[]> {
    return request<RoleItem[]>("/api/roles")
  },
  createRole(input: RoleInput): Promise<RoleItem> {
    return request<RoleItem>("/api/roles", { method: "POST", body: jsonBody(input) })
  },
  updateRole(id: number, input: RoleInput): Promise<RoleItem> {
    return request<RoleItem>(`/api/roles/${id}`, { method: "PUT", body: jsonBody(input) })
  },
  setRoleStatus(id: number, status: UserStatus): Promise<RoleItem> {
    return request<RoleItem>(`/api/roles/${id}/status`, { method: "POST", body: jsonBody({ status }) })
  },
  deleteRole(id: number): Promise<void> {
    return request<void>(`/api/roles/${id}`, { method: "DELETE" })
  },
  listDepartments(): Promise<DepartmentNode[]> {
    return request<DepartmentNode[]>("/api/departments")
  },
  createDepartment(input: DepartmentInput): Promise<DepartmentNode> {
    return request<DepartmentNode>("/api/departments", { method: "POST", body: jsonBody(input) })
  },
  updateDepartment(id: number, input: DepartmentInput): Promise<DepartmentNode> {
    return request<DepartmentNode>(`/api/departments/${id}`, { method: "PUT", body: jsonBody(input) })
  },
  deleteDepartment(id: number): Promise<void> {
    return request<void>(`/api/departments/${id}`, { method: "DELETE" })
  },
  getDashboardSummary(): Promise<DashboardSummary> {
    return request<DashboardSummary>("/api/dashboard")
  },
  listActivities(query: ActivityQuery): Promise<PageResult<ActivityItem>> {
    return request<PageResult<ActivityItem>>(`/api/activities?${buildActivityQuery(query)}`)
  },
  listActivityOperators(): Promise<OperatorOption[]> {
    return request<OperatorOption[]>("/api/activities/operators")
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
