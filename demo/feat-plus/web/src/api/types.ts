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
  /** 一个人可以挂多个角色，权限取并集、数据范围取最宽。 */
  roleCodes: RoleCode[]
  roleNames: string[]
  /** 全部角色合并后的权限点，用来决定菜单与按钮是否出现；后端仍会独立校验。 */
  permissions: string[]
  /** 数据范围档位：1 全部 / 2 自定义 / 3 本部门 / 4 本部门及以下 / 5 仅本人 */
  dataScope: number
  /** 数据范围的中文说明，例如「本部门及以下（1 个部门）」 */
  scopeDescription: string
}

/** 「我的资料」。比 CurrentUser 多出邮箱、部门名、创建时间，少掉权限点。 */
export interface Profile {
  id: number
  username: string
  displayName: string
  email: string
  departmentName: string
  roleNames: string[]
  /** 后台按本人角色算出的数据范围说明，只读——数据范围由角色决定，不能在个人中心改 */
  scopeDescription: string
  createdTime: string
}

export interface ProfileUpdate {
  displayName: string
  email: string
}

export interface ChangePasswordInput {
  oldPassword: string
  newPassword: string
}

/** 数据范围档位与名称，与后端 DataScope 一一对应。 */
export const DATA_SCOPE_ALL = 1
export const DATA_SCOPE_CUSTOM = 2
export const DATA_SCOPE_DEPARTMENT = 3
export const DATA_SCOPE_DEPARTMENT_AND_CHILD = 4
export const DATA_SCOPE_SELF = 5

export interface DataScopeOption {
  value: number
  label: string
  description: string
}

export const DATA_SCOPE_OPTIONS: DataScopeOption[] = [
  { value: DATA_SCOPE_ALL, label: "全部数据", description: "不做任何过滤" },
  { value: DATA_SCOPE_CUSTOM, label: "自定义部门", description: "只看选中的部门" },
  { value: DATA_SCOPE_DEPARTMENT, label: "本部门", description: "只看自己所在部门" },
  { value: DATA_SCOPE_DEPARTMENT_AND_CHILD, label: "本部门及以下", description: "自己所在部门及其全部下级" },
  { value: DATA_SCOPE_SELF, label: "仅本人", description: "只看自己" },
]

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
  /** 角色编码，用于提交与筛选 */
  roleCodes: RoleCode[]
  /** 角色名称，用于展示，与 roleCodes 顺序一致 */
  roleNames: string[]
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
  /** 多角色：权限取并集、数据范围取最宽 */
  roleCodes: RoleCode[]
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
  /** 数据范围档位，取值见 DATA_SCOPE_OPTIONS */
  departmentScope: number
  scopeLabel: string
  /** 只在 departmentScope 为「自定义」时有值 */
  departmentIds: number[]
  /** 内置角色：编码不可改、不可删除；ADMIN 的权限与数据范围也不可改。 */
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
  departmentScope: number
  /** 仅当 departmentScope 为「自定义」时提交 */
  departmentIds: number[]
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
  /** 动态发生在谁身上；系统事件为 null */
  userId?: number | null
  /** 执行操作的人，系统事件为 null */
  creator?: number | null
  /** 操作人姓名，由后端联表得到；操作人已被删除时为 null */
  operatorName?: string | null
}

export interface ActivityQuery {
  page: number
  pageSize: number
  kind: "all" | ActivityKind
  operatorId: "all" | number
  /** yyyy-MM-dd，空串表示不限 */
  from: string
  /** yyyy-MM-dd，含当天 */
  to: string
}

/** 日志筛选下拉的操作人选项，来自日志表本身而不是用户表。 */
export interface OperatorOption {
  id: number
  name: string
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
