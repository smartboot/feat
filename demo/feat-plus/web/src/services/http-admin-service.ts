import type {
  ActivityItem,
  ActivityQuery,
  CurrentUser,
  DashboardSummary,
  DatasourceOption,
  DepartmentInput,
  DepartmentNode,
  DwDataQuery,
  DwRow,
  DwSaveInput,
  GroupUpdateInput,
  LoginResponse,
  MenuAdminItem,
  MenuGroup,
  MenuInput,
  NotificationItem,
  OperatorOption,
  PageAdminItem,
  PageConfig,
  PageInput,
  PageResult,
  PermissionItem,
  Profile,
  ProfileUpdate,
  ChangePasswordInput,
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

function buildDwQuery(query: DwDataQuery): string {
  const params = new URLSearchParams()
  params.set("page", String(query.page))
  params.set("pageSize", String(query.pageSize))
  // 筛选整包 JSON：值由后端数据源做类型化解释，不参与 SQL 拼接。
  if (Object.keys(query.filters).length > 0) params.set("filter", JSON.stringify(query.filters))
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
  getProfile(): Promise<Profile> {
    return request<Profile>("/api/profile")
  },
  updateProfile(input: ProfileUpdate): Promise<Profile> {
    return request<Profile>("/api/profile", { method: "PUT", body: jsonBody(input) })
  },
  changePassword(input: ChangePasswordInput): Promise<number> {
    return request<number>("/api/profile/password", { method: "POST", body: jsonBody(input) })
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
  listPermissionCatalog(): Promise<PermissionItem[]> {
    return request<PermissionItem[]>("/api/permissions")
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
  listMenus(): Promise<MenuAdminItem[]> {
    return request<MenuAdminItem[]>("/api/menus")
  },
  listMenuRoutes(): Promise<string[]> {
    return request<string[]>("/api/menus/routes")
  },
  createMenu(input: MenuInput): Promise<MenuAdminItem> {
    return request<MenuAdminItem>("/api/menus", { method: "POST", body: jsonBody(input) })
  },
  updateMenu(id: number, input: MenuInput): Promise<MenuAdminItem> {
    return request<MenuAdminItem>(`/api/menus/${id}`, { method: "PUT", body: jsonBody(input) })
  },
  deleteMenu(id: number): Promise<void> {
    return request<void>(`/api/menus/${id}`, { method: "DELETE" })
  },
  listMenuGroups(): Promise<MenuGroup[]> {
    return request<MenuGroup[]>("/api/menus/groups")
  },
  updateMenuGroup(groupKey: string, input: GroupUpdateInput): Promise<void> {
    return request<void>(`/api/menus/groups/${groupKey}`, { method: "PUT", body: jsonBody(input) })
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
  getPageConfig(pageKey: string): Promise<PageConfig> {
    return request<PageConfig>(`/api/dw/pages/${encodeURIComponent(pageKey)}`)
  },
  /** 列表数据。queryPath 是数据通道覆盖（配置里的 dataApi.query），留空走内置通道。 */
  listDwData(pageKey: string, query: DwDataQuery, queryPath?: string): Promise<PageResult<DwRow>> {
    const path = queryPath || `/api/dw/pages/${encodeURIComponent(pageKey)}/data`
    return request<PageResult<DwRow>>(`${path}?${buildDwQuery(query)}`)
  },
  getDwRow(pageKey: string, id: number): Promise<DwRow> {
    return request<DwRow>(`/api/dw/pages/${encodeURIComponent(pageKey)}/data/${id}`)
  },
  saveDwData(pageKey: string, input: DwSaveInput): Promise<DwRow> {
    return request<DwRow>(`/api/dw/pages/${encodeURIComponent(pageKey)}/data`, {
      method: "POST",
      body: jsonBody(input),
    })
  },
  /** deletePath 覆盖内置通道（调用时拼 /{id}）。保存/详情不支持覆盖——写路径不开旁路。 */
  deleteDwRow(pageKey: string, id: number, deletePath?: string): Promise<void> {
    const path = deletePath || `/api/dw/pages/${encodeURIComponent(pageKey)}/data`
    return request<void>(`${path}/${id}`, { method: "DELETE" })
  },
  listPageConfigs(): Promise<PageAdminItem[]> {
    return request<PageAdminItem[]>("/api/pages")
  },
  createPageConfig(input: PageInput): Promise<PageAdminItem> {
    return request<PageAdminItem>("/api/pages", { method: "POST", body: jsonBody(input) })
  },
  updatePageConfig(id: number, input: PageInput): Promise<PageAdminItem> {
    return request<PageAdminItem>(`/api/pages/${id}`, { method: "PUT", body: jsonBody(input) })
  },
  deletePageConfig(id: number): Promise<void> {
    return request<void>(`/api/pages/${id}`, { method: "DELETE" })
  },
  listDatasources(): Promise<DatasourceOption[]> {
    return request<DatasourceOption[]>("/api/pages/datasources")
  },
}
