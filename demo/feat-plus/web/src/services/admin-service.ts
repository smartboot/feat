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
  getDashboardSummary(): Promise<DashboardSummary>
  listNotifications(): Promise<NotificationItem[]>
  markNotificationRead(id: number): Promise<void>
  markAllNotificationsRead(): Promise<void>
  resetDemo(): Promise<void>
}
