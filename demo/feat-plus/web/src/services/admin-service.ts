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

/**
 * 前后端共同遵守的契约。真实实现在 http-admin-service，页面只依赖这个接口。
 */
export interface AdminService {
  login(username: string, password: string): Promise<LoginResponse>
  currentUser(): Promise<CurrentUser>
  listUsers(query: UserQuery): Promise<PageResult<UserListItem>>
  getAllUsers(): Promise<UserListItem[]>
  getUser(id: number): Promise<UserListItem>
  createUser(input: UserInput): Promise<UserListItem>
  updateUser(id: number, input: UserInput): Promise<UserListItem>
  setUserStatus(id: number, status: UserStatus): Promise<UserListItem>
  resetUserPassword(id: number): Promise<{ temporaryPassword: string }>
  deleteUser(id: number): Promise<void>
  listRoles(): Promise<RoleItem[]>
  /** 权限点字典（含分组与说明），由后端下发；前端不再维护常量 */
  listPermissionCatalog(): Promise<PermissionItem[]>
  createRole(input: RoleInput): Promise<RoleItem>
  updateRole(id: number, input: RoleInput): Promise<RoleItem>
  setRoleStatus(id: number, status: UserStatus): Promise<RoleItem>
  deleteRole(id: number): Promise<void>
  listDepartments(): Promise<DepartmentNode[]>
  createDepartment(input: DepartmentInput): Promise<DepartmentNode>
  updateDepartment(id: number, input: DepartmentInput): Promise<DepartmentNode>
  deleteDepartment(id: number): Promise<void>
  getDashboardSummary(): Promise<DashboardSummary>
  listActivities(query: ActivityQuery): Promise<PageResult<ActivityItem>>
  listActivityOperators(): Promise<OperatorOption[]>
  listNotifications(): Promise<NotificationItem[]>
  markNotificationRead(id: number): Promise<void>
  markAllNotificationsRead(): Promise<void>
  resetDemo(): Promise<void>
  /** 个人中心：只要求登录，不要求任何权限点 */
  getProfile(): Promise<Profile>
  updateProfile(input: ProfileUpdate): Promise<Profile>
  /** 返回被踢掉的其它会话数 */
  changePassword(input: ChangePasswordInput): Promise<number>
}
