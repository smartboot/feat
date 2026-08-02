import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import type { UserInput, UserQuery, UserStatus } from "@/api/types"
import { adminService } from "@/services/mock-admin-service"

export const queryKeys = {
  users: (query: UserQuery) => ["users", query] as const,
  allUsers: ["users", "all"] as const,
  user: (id: number) => ["users", id] as const,
  dashboard: ["dashboard"] as const,
  notifications: ["notifications"] as const,
}

export function useUsers(query: UserQuery) {
  return useQuery({ queryKey: queryKeys.users(query), queryFn: () => adminService.listUsers(query), placeholderData: (previous) => previous })
}

export function useAllUsers(enabled = true) {
  return useQuery({ queryKey: queryKeys.allUsers, queryFn: () => adminService.getAllUsers(), enabled })
}

export function useUser(id: number, enabled = true) {
  return useQuery({ queryKey: queryKeys.user(id), queryFn: () => adminService.getUser(id), enabled })
}

export function useDashboard() {
  return useQuery({ queryKey: queryKeys.dashboard, queryFn: () => adminService.getDashboardSummary() })
}

export function useNotifications() {
  return useQuery({ queryKey: queryKeys.notifications, queryFn: () => adminService.listNotifications() })
}

export function useUserMutations() {
  const client = useQueryClient()
  const invalidate = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: ["users"] }),
      client.invalidateQueries({ queryKey: queryKeys.dashboard }),
      client.invalidateQueries({ queryKey: queryKeys.notifications }),
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

export function useNotificationMutations() {
  const client = useQueryClient()
  const invalidate = () => client.invalidateQueries({ queryKey: queryKeys.notifications })
  return {
    markRead: useMutation({ mutationFn: (id: number) => adminService.markNotificationRead(id), onSuccess: invalidate }),
    markAll: useMutation({ mutationFn: () => adminService.markAllNotificationsRead(), onSuccess: invalidate }),
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
