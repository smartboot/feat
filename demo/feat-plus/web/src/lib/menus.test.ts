import { describe, expect, it } from "vitest"

import type { MenuItem } from "@/api/types"
import { findMenuName, groupByKey, navGroups } from "@/lib/menus"

const menu = (path: string, name: string, groupKey: string, groupLabel: string, sort: number, visible = true): MenuItem => ({
  groupKey,
  groupLabel,
  path,
  name,
  icon: "dashboard",
  permission: "",
  sort,
  visible,
})

const menus: MenuItem[] = [
  menu("/dashboard", "数据概览", "workspace", "工作台", 10),
  menu("/users", "用户管理", "workspace", "工作台", 20),
  menu("/roles", "角色权限", "system", "系统管理", 10),
  menu("/profile", "个人中心", "account", "账号", 10, false),
]

describe("menu grouping", () => {
  it("组顺序取第一次出现的先后，组内按 sort 升序", () => {
    const shuffled = [menus[2], menus[1], menus[0]]
    const groups = groupByKey(shuffled)
    expect(groups.map((group) => group.key)).toEqual(["system", "workspace"])
    expect(groups[1].items.map((item) => item.path)).toEqual(["/dashboard", "/users"])
  })

  it("visible=false 的菜单不进侧栏，但仍然是页面标题的来源", () => {
    expect(navGroups(menus).flatMap((group) => group.items.map((item) => item.path))).not.toContain("/profile")
    expect(findMenuName(menus, "/profile")).toBe("个人中心")
  })
})

describe("页面标题", () => {
  it("详情路由按前缀归到列表菜单", () => {
    expect(findMenuName(menus, "/users/12")).toBe("用户管理")
  })

  it("精确匹配优先，前缀不跨路径段", () => {
    expect(findMenuName(menus, "/users")).toBe("用户管理")
    expect(findMenuName([...menus, menu("/users-archive", "归档", "workspace", "工作台", 30)], "/users-archive")).toBe("归档")
    expect(findMenuName(menus, "/users-archive")).toBeUndefined()
  })
})
