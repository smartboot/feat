import { describe, expect, it } from "vitest"

import { ALL_PERMISSIONS, hasAnyPermission, hasPermission, permissionLabel, sortPermissions } from "@/lib/permissions"

const admin = { permissions: ["dashboard:read", "user:read", "user:write", "role:read"] }
const auditor = { permissions: ["dashboard:read", "user:read"] }

describe("permission helpers", () => {
  it("按权限点判断，未知用户一律拒绝", () => {
    expect(hasPermission(admin, "user:write")).toBe(true)
    expect(hasPermission(auditor, "user:write")).toBe(false)
    expect(hasPermission(null, "user:read")).toBe(false)
  })

  it("任一命中即可", () => {
    expect(hasAnyPermission(auditor, ["user:write", "user:read"])).toBe(true)
    expect(hasAnyPermission(auditor, ["role:read", "dept:write"])).toBe(false)
  })

  it("权限点按字典顺序排序，未知 key 被丢弃", () => {
    expect(sortPermissions(["role:read", "dashboard:read", "hack:all"])).toEqual(["dashboard:read", "role:read"])
  })

  it("字典里的 key 不重复，标签可按 key 反查", () => {
    const keys = ALL_PERMISSIONS.map((item) => item.key)
    expect(new Set(keys).size).toBe(keys.length)
    expect(permissionLabel("dept:write")).toBe("管理部门")
    expect(permissionLabel("unknown:key")).toBe("unknown:key")
  })
})
