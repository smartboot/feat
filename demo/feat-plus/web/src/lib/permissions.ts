/**
 * 权限点字典（与后端 Permission 类一一对应）。
 *
 * 前端拿它做两件事：渲染角色页的勾选项、判断菜单和按钮是否出现。
 * 但页面隐藏只是体验——真正能拦住请求的是后端每个接口上的 require(permission)。
 */
export type PermissionKey =
  | "dashboard:read"
  | "user:read"
  | "user:write"
  | "role:read"
  | "role:write"
  | "dept:read"
  | "dept:write"
  | "audit:read"
  | "system:write"

export interface PermissionSpec {
  key: PermissionKey
  label: string
  description: string
}

export interface PermissionGroup {
  key: string
  label: string
  items: PermissionSpec[]
}

export const PERMISSION_GROUPS: PermissionGroup[] = [
  {
    key: "dashboard",
    label: "数据概览",
    items: [{ key: "dashboard:read", label: "查看概览", description: "访问首页仪表盘与登录趋势" }],
  },
  {
    key: "user",
    label: "成员管理",
    items: [
      { key: "user:read", label: "查看成员", description: "查看成员列表与详情" },
      { key: "user:write", label: "管理成员", description: "新建、编辑、停用、删除与重置密码" },
    ],
  },
  {
    key: "role",
    label: "角色权限",
    items: [
      { key: "role:read", label: "查看角色", description: "查看角色列表与权限点" },
      { key: "role:write", label: "管理角色", description: "新建、编辑、停用与删除角色" },
    ],
  },
  {
    key: "dept",
    label: "部门管理",
    items: [
      { key: "dept:read", label: "查看部门", description: "查看部门树与成员数" },
      { key: "dept:write", label: "管理部门", description: "新建、调整层级与删除部门" },
    ],
  },
  {
    key: "audit",
    label: "审计日志",
    items: [{ key: "audit:read", label: "查看审计日志", description: "查看谁在什么时候改了谁，只读" }],
  },
  {
    key: "system",
    label: "系统",
    items: [{ key: "system:write", label: "重置演示数据", description: "清空并重建全部演示数据" }],
  },
]

export const ALL_PERMISSIONS: PermissionSpec[] = PERMISSION_GROUPS.flatMap((group) => group.items)

export function permissionLabel(key: string): string {
  return ALL_PERMISSIONS.find((item) => item.key === key)?.label ?? key
}

/** 权限点展示顺序与后端一致，便于两边比对。 */
export function sortPermissions(keys: string[]): string[] {
  const order = ALL_PERMISSIONS.map((item) => item.key as string)
  return [...keys].filter((key) => order.includes(key)).sort((a, b) => order.indexOf(a) - order.indexOf(b))
}

export function hasPermission(user: { permissions: string[] } | null | undefined, key: PermissionKey): boolean {
  if (!user) return false
  return user.permissions.includes(key)
}

export function hasAnyPermission(user: { permissions: string[] } | null | undefined, keys: PermissionKey[]): boolean {
  return keys.some((key) => hasPermission(user, key))
}
