import { describe, expect, it } from "vitest"

import { hasAnyPermission, hasPermission } from "@/lib/permissions"

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

  // 字典改成后端下发之后，前端没有枚举可查，拼错 key 不会编译失败。
  // 这里把后果钉死：恒为 false，表现为按钮不出现——所以权限点的正确性由
  // scripts/check_menu.py（注册表 key 必须存在于后端字典）兜，而不是靠类型。
  it("拼错的权限点恒为 false", () => {
    expect(hasPermission(admin, "user:wirte")).toBe(false)
  })
})
