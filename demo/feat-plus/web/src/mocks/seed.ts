import type { ActivityItem, CurrentUser, NotificationItem, TrendPoint, UserListItem } from "@/api/types"

export const demoUser: CurrentUser = {
  id: 1,
  username: "admin",
  displayName: "管理员",
  roleCode: "ADMIN",
}

export const seedUsers: UserListItem[] = [
  { id: 1, username: "admin", displayName: "系统管理员", roleCode: "ADMIN", department: "平台研发部", email: "admin@feat.plus", status: 1, createdAt: "2026-07-18 09:12", updatedAt: "2026-08-02 09:20" },
  { id: 2, username: "alice", displayName: "陈晓雨", roleCode: "USER", department: "产品设计部", email: "alice@feat.plus", status: 1, createdAt: "2026-07-22 11:30", updatedAt: "2026-08-01 16:15" },
  { id: 3, username: "bob", displayName: "周明远", roleCode: "USER", department: "客户成功部", email: "bob@feat.plus", status: 1, createdAt: "2026-07-23 14:02", updatedAt: "2026-07-30 10:42" },
  { id: 4, username: "carol", displayName: "林书雅", roleCode: "AUDITOR", department: "财务审计部", email: "carol@feat.plus", status: 1, createdAt: "2026-07-24 16:48", updatedAt: "2026-08-01 11:08" },
  { id: 5, username: "david", displayName: "郑可为", roleCode: "USER", department: "平台研发部", email: "david@feat.plus", status: 0, createdAt: "2026-07-25 10:20", updatedAt: "2026-08-02 08:32" },
  { id: 6, username: "ella", displayName: "苏婉宁", roleCode: "USER", department: "市场运营部", email: "ella@feat.plus", status: 1, createdAt: "2026-07-26 17:35", updatedAt: "2026-07-31 15:46" },
  { id: 7, username: "frank", displayName: "王逸飞", roleCode: "USER", department: "客户成功部", email: "frank@feat.plus", status: 1, createdAt: "2026-07-27 13:15", updatedAt: "2026-08-01 09:18" },
  { id: 8, username: "grace", displayName: "许清和", roleCode: "AUDITOR", department: "财务审计部", email: "grace@feat.plus", status: 0, createdAt: "2026-07-28 08:46", updatedAt: "2026-08-01 17:22" },
  { id: 9, username: "henry", displayName: "赵嘉树", roleCode: "USER", department: "市场运营部", email: "henry@feat.plus", status: 1, createdAt: "2026-07-29 15:12", updatedAt: "2026-08-02 08:55" },
  { id: 10, username: "iris", displayName: "唐语汐", roleCode: "USER", department: "产品设计部", email: "iris@feat.plus", status: 1, createdAt: "2026-07-30 12:06", updatedAt: "2026-08-01 14:03" },
  { id: 11, username: "jason", displayName: "蒋文博", roleCode: "USER", department: "平台研发部", email: "jason@feat.plus", status: 1, createdAt: "2026-08-01 09:40", updatedAt: "2026-08-02 09:06" },
  { id: 12, username: "kelly", displayName: "陆安然", roleCode: "USER", department: "产品设计部", email: "kelly@feat.plus", status: 1, createdAt: "2026-08-02 08:18", updatedAt: "2026-08-02 08:18" },
]

export const trend7: TrendPoint[] = [
  { label: "7/27", value: 42 },
  { label: "7/28", value: 48 },
  { label: "7/29", value: 45 },
  { label: "7/30", value: 61 },
  { label: "7/31", value: 58 },
  { label: "8/1", value: 72 },
  { label: "8/2", value: 68 },
]

export const trend30: TrendPoint[] = Array.from({ length: 30 }, (_, index) => ({
  label: `${index < 29 ? "7" : "8"}/${index < 29 ? index + 3 : 1}`,
  value: 36 + ((index * 11) % 31) + (index > 20 ? 8 : 0),
}))

export const seedActivities: ActivityItem[] = [
  { id: 4, kind: "enable", title: "管理员启用了用户 jason", description: "账号已恢复访问工作空间", createdAt: "2026-08-02 09:06", userId: 11 },
  { id: 3, kind: "update", title: "陈晓雨更新了个人信息", description: "邮箱和所属部门已同步", createdAt: "2026-08-02 08:48", userId: 2 },
  { id: 2, kind: "system", title: "审计员导出了登录日志", description: "导出范围为最近 30 天", createdAt: "2026-08-02 08:02", userId: 4 },
  { id: 1, kind: "system", title: "系统完成每日数据备份", description: "演示环境状态正常", createdAt: "2026-08-02 07:10" },
]

export const seedNotifications: NotificationItem[] = [
  { id: 3, title: "2 个账号处于停用状态", description: "可前往用户管理查看并处理", createdAt: "2026-08-02 09:15", read: false },
  { id: 2, title: "每日备份已完成", description: "演示数据快照创建成功", createdAt: "2026-08-02 07:10", read: false },
  { id: 1, title: "欢迎使用 Feat Plus", description: "命令面板和用户管理功能现已可用", createdAt: "2026-08-01 18:00", read: true },
]
