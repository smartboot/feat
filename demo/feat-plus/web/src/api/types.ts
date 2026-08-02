export interface RestResult<T> {
  success: boolean
  code: number
  message: string | null
  data: T
}

export type RoleCode = "ADMIN" | "USER" | "AUDITOR"
export type UserStatus = 0 | 1

export interface CurrentUser {
  id: number
  username: string
  displayName: string
  roleCode: RoleCode
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
  department: string
  email: string
  status: UserStatus
  createdAt: string
  updatedAt: string
}

export interface UserInput {
  username: string
  displayName: string
  roleCode: RoleCode
  department: string
  email: string
  status: UserStatus
}

export interface UserQuery {
  page: number
  pageSize: number
  keyword: string
  status: "all" | "0" | "1"
  roleCode: "all" | RoleCode
  department: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
}

export type ActivityKind = "create" | "update" | "enable" | "disable" | "password" | "delete" | "system"

export interface ActivityItem {
  id: number
  kind: ActivityKind
  title: string
  description: string
  createdAt: string
  userId?: number
}

export interface NotificationItem {
  id: number
  title: string
  description: string
  createdAt: string
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
