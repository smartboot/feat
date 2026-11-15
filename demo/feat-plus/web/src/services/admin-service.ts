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
  /** 菜单配置：全量（含不进侧栏的项），只有 menu:read 能读 */
  listMenus(): Promise<MenuAdminItem[]>
  /** 可选的页面路径白名单，用来把「路径」渲染成下拉框而不是自由输入 */
  listMenuRoutes(): Promise<string[]>
  createMenu(input: MenuInput): Promise<MenuAdminItem>
  updateMenu(id: number, input: MenuInput): Promise<MenuAdminItem>
  deleteMenu(id: number): Promise<void>
  /** 分组列表。聚合自 sys_menu，count=0 表示空组 */
  listMenuGroups(): Promise<MenuGroup[]>
  /** 更新分组的名称和顺序 */
  updateMenuGroup(groupKey: string, input: GroupUpdateInput): Promise<void>
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
  /** 动态页面：拉渲染配置（权限按数据源声明，canWrite 逐请求计算） */
  getPageConfig(pageKey: string): Promise<PageConfig>
  /** 动态页面：列表数据（分页 + 筛选）。queryPath 覆盖内置通道，见 DwDataApi */
  listDwData(pageKey: string, query: DwDataQuery, queryPath?: string): Promise<PageResult<DwRow>>
  /** 动态页面：单行数据，表单编辑回填 */
  getDwRow(pageKey: string, id: number): Promise<DwRow>
  /** 动态页面：保存（id 为空表示新建） */
  saveDwData(pageKey: string, input: DwSaveInput): Promise<DwRow>
  /** deletePath 覆盖内置通道（调用时拼 /{id}）。保存/详情不支持覆盖——写路径不开旁路 */
  deleteDwRow(pageKey: string, id: number, deletePath?: string): Promise<void>
  /**
   * 自定义行操作：POST 到配置里的站内接口，请求体是 `{ [rowKey]: id }`。
   * 返回后端的 message（没有就返回空串），由调用方决定怎么提示。
   */
  runDwRowAction(api: string, rowKey: string, id: number): Promise<string>
  /** 页面配置管理 */
  listPageConfigs(): Promise<PageAdminItem[]>
  createPageConfig(input: PageInput): Promise<PageAdminItem>
  updatePageConfig(id: number, input: PageInput): Promise<PageAdminItem>
  deletePageConfig(id: number): Promise<void>
  /** 可选数据源 + 字段声明，配置表单的下拉与字段提示 */
  listDatasources(): Promise<DatasourceOption[]>
}
