import type { MenuItem } from "@/api/types"

/**
 * 菜单与权限字典共用的分组逻辑。
 *
 * 后端下发的是「扁平的、带 groupKey 的列表」而不是嵌套结构：扁平的好处是 DTO 只有一层，
 * AOT 序列化不用处理嵌套泛型；分组放在前端也只是一次 O(n) 遍历。
 */
export interface GroupedItems<T> {
  key: string
  label: string
  items: T[]
}

interface Groupable {
  groupKey: string
  groupLabel: string
  sort: number
}

/**
 * 按 groupKey 分组：组顺序取该组第一次出现的先后，组内按 sort 升序。
 *
 * 组顺序不用 sort 排序，是因为 sort 在组内也有意义（10/20/30），
 * 拿它给组排序会让「工作台 10、系统管理 10」这种并列没法表达。
 */
export function groupByKey<T extends Groupable>(items: T[]): GroupedItems<T>[] {
  const groups: GroupedItems<T>[] = []
  const index = new Map<string, GroupedItems<T>>()
  for (const item of items) {
    let group = index.get(item.groupKey)
    if (!group) {
      group = { key: item.groupKey, label: item.groupLabel, items: [] }
      index.set(item.groupKey, group)
      groups.push(group)
    }
    group.items.push(item)
  }
  for (const group of groups) {
    group.items.sort((a, b) => a.sort - b.sort)
  }
  return groups
}

/** 页面标题用的菜单，只看进入侧栏的那些——个人中心这类不进侧栏，但也要有标题。 */
export function navGroups(menus: MenuItem[]): GroupedItems<MenuItem>[] {
  return groupByKey(menus.filter((item) => item.visible))
}

/**
 * 按路径找菜单名。用最长前缀匹配，因为列表页的详情路由（/users/12）不在注册表里，
 * 但它属于 /users 这一项；精确匹配优先，避免 /users-archive 被 /users 吃掉。
 */
export function findMenuName(menus: MenuItem[], pathname: string): string | undefined {
  const exact = menus.find((item) => item.path === pathname)
  if (exact) return exact.name

  let best: MenuItem | undefined
  for (const item of menus) {
    if (item.path === "/") continue
    const hit = pathname === item.path || pathname.startsWith(`${item.path}/`)
    if (hit && (!best || item.path.length > best.path.length)) best = item
  }
  return best?.name
}
