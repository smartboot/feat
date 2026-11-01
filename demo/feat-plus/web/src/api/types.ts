export interface RestResult<T> {
  success: boolean
  code: number
  message: string | null
  data: T
}

/** 角色编码不再写死成联合类型：角色可以在「角色权限」页自行新增。 */
export type RoleCode = string
export type UserStatus = 0 | 1

export interface CurrentUser {
  id: number
  username: string
  displayName: string
  roleCode: RoleCode
  roleName: string
  /** 角色生效的权限点，用来决定菜单与按钮是否出现；后端仍会独立校验。 */
  permissions: string[]
}

export interface LoginResponse {
  accessToken: string
  tokenType: "Bearer"
  expiresIn: number
  user: CurrentUser
}

export interface UserListItem {
  id: number
  username: string
  displayName: string
  roleCode: RoleCode
  /** 所属部门主键；历史数据可能为空。 */
  departmentId: number | null
  /** 部门名称来自后端联表查询，只用于展示与搜索。 */
  departmentName: string
  email: string
  status: UserStatus
  createdTime: string
  editTime: string
  /** 创建人 / 最后编辑人的用户 id，系统初始化的种子数据为 null */
  creator?: number | null
  editor?: number | null
}

export interface UserInput {
  username: string
  displayName: string
  roleCode: RoleCode
  departmentId: number | null
  email: string
  status: UserStatus
}

export interface UserQuery {
  page: number
  pageSize: number
  keyword: string
  status: "all" | "0" | "1"
  roleCode: "all" | RoleCode
  /** all 表示不限部门；数字表示该部门及其全部下级。 */
  departmentId: "all" | number
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
}

export interface RoleItem {
  id: number
  code: RoleCode
  name: string
  description: string
  permissions: string[]
  /** 内置角色：编码不可改、不可删除；ADMIN 的权限也不可改。 */
  builtin: boolean
  status: UserStatus
  /** 该角色下的账号数，用于删除前提示。 */
  userCount: number
  createdTime: string
  editTime: string
}

export interface RoleInput {
  code: string
  name: string
  description: string
  permissions: string[]
  status: UserStatus
}

export interface DepartmentNode {
  id: number
  parentId: number
  code: string
  name: string
  sortOrder: number
  description: string
  status: UserStatus
  /** 直属成员数，不含子部门 */
  userCount: number
  createdTime: string
  editTime: string
  children: DepartmentNode[]
}

export interface DepartmentInput {
  parentId: number
  code: string
  name: string
  sortOrder: number
  description: string
  status: UserStatus
}

/** 部门树拍平后的选项，depth 用于在下拉里做层级缩进。 */
export interface DepartmentOption {
  id: number
  name: string
  depth: number
  disabled: boolean
}

export type ActivityKind = "create" | "update" | "enable" | "disable" | "password" | "delete" | "system"

export interface ActivityItem {
  id: number
  kind: ActivityKind
  title: string
  description: string
  createdTime: string
  userId?: number
}

export interface NotificationItem {
  id: number
  title: string
  description: string
  createdTime: string
  read: boolean
  userId?: number
}

export interface TrendPoint {
  label: string
  value: number
}

export interface DashboardSummary {
  totalUsers: number
  enabledUsers: number
  disabledUsers: number
  unreadNotifications: number
  trend7: TrendPoint[]
  trend30: TrendPoint[]
  activities: ActivityItem[]
}
