import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import type {
  ActivityQuery,
  ChangePasswordInput,
  DepartmentInput,
  DwDataApi,
  DwDataQuery,
  DwSaveInput,
  GroupUpdateInput,
  MenuInput,
  MenuGroup,
  PageInput,
  ProfileUpdate,
  RoleInput,
  UserInput,
  UserQuery,
  UserStatus,
} from "@/api/types"
import { adminService } from "@/services/http-admin-service"
import { request } from "@/api/client"

export const queryKeys = {
  users: (query: UserQuery) => ["users", query] as const,
  allUsers: ["users", "all"] as const,
  user: (id: number) => ["users", id] as const,
  roles: ["roles"] as const,
  permissionCatalog: ["permissions", "catalog"] as const,
  menus: ["menus"] as const,
  menuRoutes: ["menus", "routes"] as const,
  menuGroups: ["menus", "groups"] as const,
  departments: ["departments"] as const,
  dashboard: ["dashboard"] as const,
  notifications: ["notifications"] as const,
  activities: (query: ActivityQuery) => ["activities", query] as const,
  profile: ["profile"] as const,
  activityOperators: ["activities", "operators"] as const,
  // 动态页面：配置 / 数据 / 配置管理三条缓存线
  pageConfig: (pageKey: string) => ["dw", "pages", pageKey] as const,
  pageAdmin: ["dw", "admin", "pages"] as const,
  datasources: ["dw", "datasources"] as const,
  // 远程选项：同一路径 + 同一取值/显示/分组字段组合共用一个缓存，不同页面不重复拉
  remoteOptions: (apiPath: string, valueField: string, labelField: string,
                  groupField = "", groupLabelField = "") =>
    ["dw", "options", apiPath, valueField, labelField, groupField, groupLabelField] as const,
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

/**
 * 权限点字典：随发版才变，缓存久一点（5 分钟）。
 * 它只在角色页用到，所以不放进 /me——菜单里已经带了各自需要的权限点。
 */
export function usePermissionCatalog(enabled = true) {
  return useQuery({
    queryKey: queryKeys.permissionCatalog,
    queryFn: () => adminService.listPermissionCatalog(),
    enabled,
    staleTime: 5 * 60_000,
  })
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

/**
 * 菜单配置列表。与侧边栏用的是同一份数据（后端同一个缓存），
 * 所以改完菜单要连当前登录人一起刷新，否则管理页显示新值、左边侧栏还是旧的。
 */
export function useMenus(enabled = true) {
  return useQuery({ queryKey: queryKeys.menus, queryFn: () => adminService.listMenus(), enabled })
}

/**
 * 可选页面路径。随发版才变（MenuRoutes 是代码常量），缓存久一点。
 */
export function useMenuRoutes(enabled = true) {
  return useQuery({
    queryKey: queryKeys.menuRoutes,
    queryFn: () => adminService.listMenuRoutes(),
    enabled,
    staleTime: 5 * 60_000,
  })
}

export function useMenuMutations(onChanged?: () => void) {
  const client = useQueryClient()
  const invalidate = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: queryKeys.menus }),
      client.invalidateQueries({ queryKey: queryKeys.menuGroups }),
      client.invalidateQueries({ queryKey: ["activities"] }),
    ])
    // 侧边栏的菜单来自 /api/auth/me 的响应，不在 react-query 缓存里，
    // 必须由调用方重新拉一次当前登录人——否则改完标题，管理页变了、左边还是旧的。
    onChanged?.()
  }
  return {
    create: useMutation({ mutationFn: (input: MenuInput) => adminService.createMenu(input), onSuccess: invalidate }),
    update: useMutation({ mutationFn: ({ id, input }: { id: number; input: MenuInput }) => adminService.updateMenu(id, input), onSuccess: invalidate }),
    remove: useMutation({ mutationFn: (id: number) => adminService.deleteMenu(id), onSuccess: invalidate }),
  }
}

/**
 * 分组列表。菜单管理页的「分组管理」面板用。
 */
export function useMenuGroups(enabled = true) {
  return useQuery({ queryKey: queryKeys.menuGroups, queryFn: () => adminService.listMenuGroups(), enabled })
}

/**
 * 分组更新。成功后同时失效菜单列表（分组信息存在菜单行上，改分组等于改菜单）。
 */
export function useMenuGroupMutations(onChanged?: () => void) {
  const client = useQueryClient()
  const invalidate = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: queryKeys.menuGroups }),
      client.invalidateQueries({ queryKey: queryKeys.menus }),
    ])
    onChanged?.()
  }
  return {
    update: useMutation(
      { mutationFn: ({ groupKey, input }: { groupKey: string; input: GroupUpdateInput }) => adminService.updateMenuGroup(groupKey, input), onSuccess: invalidate },
    ),
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

// ---------------------------------------------------------------------------
// 动态页面
// ---------------------------------------------------------------------------

/**
 * 页面渲染配置。配置低频变更（staleTime 5 分钟兜底，保存配置后主动失效），
 * 每次渲染动态页都拉它——这是「配置即页面」的唯一入口。
 */
export function usePageConfig(pageKey: string, enabled = true) {
  return useQuery({
    queryKey: queryKeys.pageConfig(pageKey),
    queryFn: () => adminService.getPageConfig(pageKey),
    staleTime: 5 * 60_000,
    enabled,
  })
}

/** 动态列表数据：翻页时保留上一页，避免表格闪白（与 useUsers 同款取舍）。
 * dataApi 是通道覆盖：query 路径进缓存键，换通道等于换数据源。 */
export function useDwData(pageKey: string, query: DwDataQuery, enabled = true, dataApi?: DwDataApi) {
  const queryPath = dataApi?.query || ""
  return useQuery({
    queryKey: ["dw", "data", pageKey, queryPath, query],
    queryFn: () => adminService.listDwData(pageKey, query, dataApi?.query),
    placeholderData: (previous) => previous,
    enabled,
  })
}

/**
 * 动态页面的写操作。只失效本页数据——配置没变，别的页面不陪跑。
 * 角色数据源背后是手写 Service，写完还会产生审计日志。
 * dataApi.delete 覆盖删除通道；保存始终走内置通道（权限由目标接口自己校验）。
 *
 * action 是配置里的自定义行操作：路径来自配置、主键来自当前行。
 * 执行完照样失效本页——「重新分配」之后列表里的归属列很可能已经变了，
 * 让用户手动刷新是最容易被漏掉的一步。
 */
export function useDwMutations(pageKey: string, dataApi?: DwDataApi) {
  const client = useQueryClient()
  const invalidate = async () => {
    await client.invalidateQueries({ queryKey: ["dw", "data", pageKey] })
    await client.invalidateQueries({ queryKey: ["activities"] })
  }
  return {
    save: useMutation({
      mutationFn: (input: DwSaveInput) => adminService.saveDwData(pageKey, input),
      onSuccess: invalidate,
    }),
    remove: useMutation({
      mutationFn: (id: number) => adminService.deleteDwRow(pageKey, id, dataApi?.delete),
      onSuccess: invalidate,
    }),
    action: useMutation({
      mutationFn: (input: { api: string; rowKey: string; id: number }) =>
        adminService.runDwRowAction(input.api, input.rowKey, input.id),
      onSuccess: invalidate,
    }),
  }
}

/** 配置管理页的列表。 */
export function usePageAdmin(enabled = true) {
  return useQuery({ queryKey: queryKeys.pageAdmin, queryFn: () => adminService.listPageConfigs(), enabled })
}

/**
 * 远程选项：筛选下拉的选项不写死在配置里，而是运行时从这个站内接口取。
 *
 * 配置只存「去哪取」和「取哪两个字段」，取回来的列表按三者组合缓存。
 * 这样角色改名、新增部门、人员变动之后，不需要重新保存页面配置——
 * 存进 sys_page 的是取数方式，不是取数的结果。
 *
 * 请求走同一个 request（带 Bearer），所以能取到哪些选项由目标接口自己判权：
 * 操作人下拉只会列出当前数据范围内的人，不会因为「配置了一个接口路径」就变成越权入口。
 */
export interface RemoteOption {
  value: string
  label: string
  /** 分组标识；没配分组字段时是空串 */
  group: string
  groupLabel: string
}

export function useRemoteOptions(
  apiPath: string,
  valueField: string,
  labelField: string,
  groupField = "",
  groupLabelField = "",
) {
  return useQuery({
    queryKey: queryKeys.remoteOptions(apiPath, valueField, labelField, groupField, groupLabelField),
    queryFn: async (): Promise<RemoteOption[]> => {
      const rows = await request<Array<Record<string, unknown>>>(apiPath)
      if (!Array.isArray(rows)) return []
      return rows
        .map((row) => {
          const group = groupField ? String(row[groupField] ?? "") : ""
          return {
            value: String(row[valueField] ?? ""),
            label: String(row[labelField] ?? ""),
            group,
            // 分组标题优先取专门的分组文案字段，没有就退回分组标识本身：
            // 权限点接口两个都给了（groupKey / groupLabel），部门接口往往只有 id。
            groupLabel: group === ""
              ? ""
              : String(row[groupLabelField || labelField] ?? group),
          }
        })
        .filter((option) => option.value !== "")
    },
    enabled: apiPath !== "",
    staleTime: 5 * 60_000,
  })
}

/** 可选数据源字典（编译期白名单），staleTime 同权限字典量级。 */
export function useDatasources(enabled = true) {
  return useQuery({
    queryKey: queryKeys.datasources,
    queryFn: () => adminService.listDatasources(),
    staleTime: 5 * 60_000,
    enabled,
  })
}

/**
 * 页面配置的写操作。失效范围比数据写操作大：配置决定所有动态页的渲染，
 * 页面管理页改完，所有 pageKey 的配置缓存与菜单下拉都要重新拉。
 */
export function usePageMutations() {
  const client = useQueryClient()
  const invalidate = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: ["dw", "pages"] }),
      client.invalidateQueries({ queryKey: ["dw", "data"] }),
      client.invalidateQueries({ queryKey: queryKeys.pageAdmin }),
      client.invalidateQueries({ queryKey: queryKeys.menuRoutes }),
      client.invalidateQueries({ queryKey: ["activities"] }),
    ])
  }
  return {
    create: useMutation({ mutationFn: (input: PageInput) => adminService.createPageConfig(input), onSuccess: invalidate }),
    update: useMutation({ mutationFn: ({ id, input }: { id: number; input: PageInput }) => adminService.updatePageConfig(id, input), onSuccess: invalidate }),
    remove: useMutation({ mutationFn: (id: number) => adminService.deletePageConfig(id), onSuccess: invalidate }),
  }
}
