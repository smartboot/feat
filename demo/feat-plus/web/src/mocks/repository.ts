import type {
  ActivityItem,
  DashboardSummary,
  NotificationItem,
  PageResult,
  UserInput,
  UserListItem,
  UserQuery,
  UserStatus,
} from "@/api/types"
import { demoUser, seedActivities, seedNotifications, seedUsers, trend30, trend7 } from "@/mocks/seed"

const STORAGE_KEY = "feat-plus-mock-db:v1"

interface MockDatabase {
  users: UserListItem[]
  activities: ActivityItem[]
  notifications: NotificationItem[]
  nextUserId: number
  nextActivityId: number
  nextNotificationId: number
}

export class MockDomainError extends Error {
  constructor(message: string) {
    super(message)
    this.name = "MockDomainError"
  }
}

let database = loadDatabase()

export function listUsers(query: UserQuery): PageResult<UserListItem> {
  const keyword = query.keyword.trim().toLowerCase()
  const filtered = database.users
    .filter((user) => {
      const keywordMatched = !keyword || [user.username, user.displayName, user.department, user.email].some((value) => value.toLowerCase().includes(keyword))
      const statusMatched = query.status === "all" || String(user.status) === query.status
      const roleMatched = query.roleCode === "all" || user.roleCode === query.roleCode
      const departmentMatched = !query.department || user.department === query.department
      return keywordMatched && statusMatched && roleMatched && departmentMatched
    })
    .sort((left, right) => right.id - left.id)
  const start = (query.page - 1) * query.pageSize
  return clone({
    items: filtered.slice(start, start + query.pageSize),
    total: filtered.length,
    page: query.page,
    pageSize: query.pageSize,
  })
}

export function getAllUsers() {
  return clone(database.users)
}

export function getUser(id: number) {
  const user = database.users.find((item) => item.id === id)
  if (!user) throw new MockDomainError("用户不存在或已被删除")
  return clone(user)
}

export function createUser(input: UserInput) {
  validateUserInput(input)
  assertUnique(input.username, input.email)
  const now = nowText()
  const user: UserListItem = {
    ...input,
    id: database.nextUserId++,
    createdAt: now,
    updatedAt: now,
  }
  database.users.push(user)
  recordEvent("create", `创建了用户 ${user.displayName}`, `${user.username} · ${roleLabel(user.roleCode)}`, user.id)
  persist()
  return clone(user)
}

export function updateUser(id: number, input: UserInput) {
  const current = requireUser(id)
  validateUserInput(input)
  assertUnique(input.username, input.email, id)
  if (current.roleCode === "ADMIN" && input.roleCode !== "ADMIN") assertNotLastEnabledAdmin(current)
  if (current.status === 1 && input.status === 0) assertCanDisable(current)
  Object.assign(current, input, { updatedAt: nowText() })
  recordEvent("update", `更新了用户 ${current.displayName}`, `${current.username} 的账号资料已变更`, current.id)
  persist()
  return clone(current)
}

export function setUserStatus(id: number, status: UserStatus) {
  const user = requireUser(id)
  if (status === 0) assertCanDisable(user)
  user.status = status
  user.updatedAt = nowText()
  recordEvent(status ? "enable" : "disable", `${status ? "启用" : "停用"}了用户 ${user.displayName}`, `${user.username} 的账号状态已更新`, user.id)
  persist()
  return clone(user)
}

export function resetUserPassword(id: number) {
  const user = requireUser(id)
  const temporaryPassword = `Feat@${String(user.id).padStart(3, "0")}8X`
  recordEvent("password", `重置了用户 ${user.displayName} 的密码`, "临时密码已生成并等待安全交付", user.id)
  persist()
  return { temporaryPassword }
}

export function deleteUser(id: number) {
  const user = requireUser(id)
  if (user.id === demoUser.id) throw new MockDomainError("不能删除当前登录账号")
  if (user.roleCode === "ADMIN" && user.status === 1) assertNotLastEnabledAdmin(user)
  database.users = database.users.filter((item) => item.id !== id)
  recordEvent("delete", `删除了用户 ${user.displayName}`, `${user.username} 已从演示工作空间移除`)
  persist()
}

export function getDashboardSummary(): DashboardSummary {
  return clone({
    totalUsers: database.users.length,
    enabledUsers: database.users.filter((user) => user.status === 1).length,
    disabledUsers: database.users.filter((user) => user.status === 0).length,
    unreadNotifications: database.notifications.filter((item) => !item.read).length,
    trend7,
    trend30,
    activities: database.activities.slice(0, 7),
  })
}

export function listNotifications() {
  return clone(database.notifications)
}

export function markNotificationRead(id: number) {
  const notification = database.notifications.find((item) => item.id === id)
  if (notification) {
    notification.read = true
    persist()
  }
}

export function markAllNotificationsRead() {
  database.notifications.forEach((item) => { item.read = true })
  persist()
}

export function resetDatabase() {
  database = createDatabase()
  persist()
}

function assertCanDisable(user: UserListItem) {
  if (user.id === demoUser.id) throw new MockDomainError("不能停用当前登录账号")
  if (user.roleCode === "ADMIN") assertNotLastEnabledAdmin(user)
}

function assertNotLastEnabledAdmin(user: UserListItem) {
  const enabledAdmins = database.users.filter((item) => item.roleCode === "ADMIN" && item.status === 1)
  if (user.status === 1 && enabledAdmins.length <= 1) throw new MockDomainError("必须至少保留一个已启用的管理员账号")
}

function assertUnique(username: string, email: string, ignoredId?: number) {
  const normalizedUsername = username.trim().toLowerCase()
  const normalizedEmail = email.trim().toLowerCase()
  if (database.users.some((user) => user.id !== ignoredId && user.username.toLowerCase() === normalizedUsername)) {
    throw new MockDomainError("用户名已存在")
  }
  if (database.users.some((user) => user.id !== ignoredId && user.email.toLowerCase() === normalizedEmail)) {
    throw new MockDomainError("邮箱已存在")
  }
}

function validateUserInput(input: UserInput) {
  if (!input.username.trim() || !input.displayName.trim() || !input.department.trim() || !input.email.trim()) {
    throw new MockDomainError("请完整填写用户信息")
  }
}

function requireUser(id: number) {
  const user = database.users.find((item) => item.id === id)
  if (!user) throw new MockDomainError("用户不存在或已被删除")
  return user
}

function recordEvent(kind: ActivityItem["kind"], title: string, description: string, userId?: number) {
  const createdAt = nowText()
  database.activities.unshift({ id: database.nextActivityId++, kind, title: `管理员${title}`, description, createdAt, userId })
  database.notifications.unshift({ id: database.nextNotificationId++, title, description, createdAt, read: false, userId })
}

function roleLabel(roleCode: UserListItem["roleCode"]) {
  return roleCode === "ADMIN" ? "管理员" : roleCode === "AUDITOR" ? "审计员" : "普通用户"
}

function loadDatabase(): MockDatabase {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY)
    if (stored) return JSON.parse(stored) as MockDatabase
  } catch {
    window.localStorage.removeItem(STORAGE_KEY)
  }
  return createDatabase()
}

function createDatabase(): MockDatabase {
  return {
    users: clone(seedUsers),
    activities: clone(seedActivities),
    notifications: clone(seedNotifications),
    nextUserId: Math.max(...seedUsers.map((item) => item.id)) + 1,
    nextActivityId: Math.max(...seedActivities.map((item) => item.id)) + 1,
    nextNotificationId: Math.max(...seedNotifications.map((item) => item.id)) + 1,
  }
}

function persist() {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(database))
}

function clone<T>(value: T): T {
  return structuredClone(value)
}

function nowText() {
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date()).replaceAll("/", "-")
}
