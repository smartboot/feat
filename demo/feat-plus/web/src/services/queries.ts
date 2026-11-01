import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import type { ActivityQuery, ChangePasswordInput, DepartmentInput, ProfileUpdate, RoleInput, UserInput, UserQuery, UserStatus } from "@/api/types"
import { adminService } from "@/services/http-admin-service"

export const queryKeys = {
  users: (query: UserQuery) => ["users", query] as const,
  allUsers: ["users", "all"] as const,
  user: (id: number) => ["users", id] as const,
  roles: ["roles"] as const,
  departments: ["departments"] as const,
  dashboard: ["dashboard"] as const,
  notifications: ["notifications"] as const,
  activities: (query: ActivityQuery) => ["activities", query] as const,
  profile: ["profile"] as const,
  activityOperators: ["activities", "operators"] as const,
}

export function useUsers(query: UserQuery, enabled = true) {
  return useQuery({
    queryKey: queryKeys.users(query),
    queryFn: () => adminService.listUsers(query),
    placeholderData: (previous) => previous,
    enabled,
  })
}

export function useAllUsers(enabled = true) {
  return useQuery({ queryKey: queryKeys.allUsers, queryFn: () => adminService.getAllUsers(), enabled })
}

export function useUser(id: number, enabled = true) {
  return useQuery({ queryKey: queryKeys.user(id), queryFn: () => adminService.getUser(id), enabled })
}

/**
 * 角色与部门变动频率低、被多个页面复用（用户表单的下拉、侧边栏），
 * 因此统一缓存 30 秒，写操作后主动失效。
 */
export function useRoles(enabled = true) {
  return useQuery({ queryKey: queryKeys.roles, queryFn: () => adminService.listRoles(), enabled, staleTime: 30_000 })
}

export function useDepartments(enabled = true) {
  return useQuery({ queryKey: queryKeys.departments, queryFn: () => adminService.listDepartments(), enabled, staleTime: 30_000 })
}

export function useDashboard() {
  return useQuery({ queryKey: queryKeys.dashboard, queryFn: () => adminService.getDashboardSummary() })
}

export function useNotifications(enabled = true) {
  return useQuery({ queryKey: queryKeys.notifications, queryFn: () => adminService.listNotifications(), enabled })
}

/** 操作日志：翻页时保留上一页数据，避免表格闪白。 */
export function useActivities(query: ActivityQuery, enabled = true) {
  return useQuery({
    queryKey: queryKeys.activities(query),
    queryFn: () => adminService.listActivities(query),
    placeholderData: (previous) => previous,
    enabled,
  })
}

export function useActivityOperators(enabled = true) {
  return useQuery({
    queryKey: queryKeys.activityOperators,
    queryFn: () => adminService.listActivityOperators(),
    enabled,
    staleTime: 30_000,
  })
}

export function useUserMutations() {
  const client = useQueryClient()
  const invalidate = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: ["users"] }),
      client.invalidateQueries({ queryKey: queryKeys.roles }),
      client.invalidateQueries({ queryKey: queryKeys.departments }),
      client.invalidateQueries({ queryKey: queryKeys.dashboard }),
      client.invalidateQueries({ queryKey: queryKeys.notifications }),
      // 任何写操作都会新增一条日志，日志列表与操作人下拉都要跟着刷新。
      client.invalidateQueries({ queryKey: ["activities"] }),
    ])
  }
  return {
    create: useMutation({ mutationFn: (input: UserInput) => adminService.createUser(input), onSuccess: invalidate }),
    update: useMutation({ mutationFn: ({ id, input }: { id: number; input: UserInput }) => adminService.updateUser(id, input), onSuccess: invalidate }),
    status: useMutation({ mutationFn: ({ id, status }: { id: number; status: UserStatus }) => adminService.setUserStatus(id, status), onSuccess: invalidate }),
    resetPassword: useMutation({ mutationFn: (id: number) => adminService.resetUserPassword(id), onSuccess: invalidate }),
    remove: useMutation({ mutationFn: (id: number) => adminService.deleteUser(id), onSuccess: invalidate }),
  }
}

export function useRoleMutations() {
  const client = useQueryClient()
  const invalidate = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: queryKeys.roles }),
      client.invalidateQueries({ queryKey: ["users"] }),
      client.invalidateQueries({ queryKey: ["activities"] }),
    ])
  }
  return {
    create: useMutation({ mutationFn: (input: RoleInput) => adminService.createRole(input), onSuccess: invalidate }),
    update: useMutation({ mutationFn: ({ id, input }: { id: number; input: RoleInput }) => adminService.updateRole(id, input), onSuccess: invalidate }),
    status: useMutation({ mutationFn: ({ id, status }: { id: number; status: UserStatus }) => adminService.setRoleStatus(id, status), onSuccess: invalidate }),
    remove: useMutation({ mutationFn: (id: number) => adminService.deleteRole(id), onSuccess: invalidate }),
  }
}

export function useDepartmentMutations() {
  const client = useQueryClient()
  const invalidate = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: queryKeys.departments }),
      client.invalidateQueries({ queryKey: ["users"] }),
      client.invalidateQueries({ queryKey: ["activities"] }),
    ])
  }
  return {
    create: useMutation({ mutationFn: (input: DepartmentInput) => adminService.createDepartment(input), onSuccess: invalidate }),
    update: useMutation({ mutationFn: ({ id, input }: { id: number; input: DepartmentInput }) => adminService.updateDepartment(id, input), onSuccess: invalidate }),
    remove: useMutation({ mutationFn: (id: number) => adminService.deleteDepartment(id), onSuccess: invalidate }),
  }
}

export function useNotificationMutations() {
  const client = useQueryClient()
  const invalidate = () => client.invalidateQueries({ queryKey: queryKeys.notifications })
  return {
    markRead: useMutation({ mutationFn: (id: number) => adminService.markNotificationRead(id), onSuccess: invalidate }),
    markAll: useMutation({ mutationFn: () => adminService.markAllNotificationsRead(), onSuccess: invalidate }),
  }
}

/**
 * 个人资料。与 /api/auth/me 是两份数据：me 决定菜单与按钮，profile 只用来渲染个人中心。
 * 改完资料后页面会额外刷新一次 me，否则侧边栏头像旁的名字还是旧的。
 */
export function useProfile() {
  return useQuery({ queryKey: queryKeys.profile, queryFn: () => adminService.getProfile() })
}

export function useProfileMutations() {
  const client = useQueryClient()
  const invalidate = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: queryKeys.profile }),
      // 昵称改了会出现在用户列表与审计日志的操作人列里。
      client.invalidateQueries({ queryKey: ["users"] }),
      client.invalidateQueries({ queryKey: ["activities"] }),
    ])
  }
  return {
    update: useMutation({ mutationFn: (input: ProfileUpdate) => adminService.updateProfile(input), onSuccess: invalidate }),
    changePassword: useMutation({ mutationFn: (input: ChangePasswordInput) => adminService.changePassword(input) }),
  }
}

export function useResetDemo() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: () => adminService.resetDemo(),
    onSuccess: async () => {
      await client.invalidateQueries()
    },
  })
}
