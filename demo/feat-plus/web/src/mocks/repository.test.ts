import { beforeEach, describe, expect, it } from "vitest"
import type { UserInput } from "@/api/types"
import { createUser, deleteUser, getDashboardSummary, listUsers, resetDatabase, setUserStatus } from "@/mocks/repository"

const input: UserInput = { username: "new-user", displayName: "新成员", email: "new-user@feat.plus", department: "平台研发部", roleCode: "USER", status: 1 }

describe("mock admin repository", () => {
  beforeEach(() => {
    window.localStorage.clear()
    resetDatabase()
  })

  it("filters and paginates users", () => {
    const result = listUsers({ page: 1, pageSize: 2, keyword: "平台", status: "all", roleCode: "all", department: "" })
    expect(result.total).toBe(3)
    expect(result.items).toHaveLength(2)
  })

  it("updates dashboard counts after mutations", () => {
    const before = getDashboardSummary()
    const created = createUser(input)
    expect(getDashboardSummary().totalUsers).toBe(before.totalUsers + 1)
    setUserStatus(created.id, 0)
    expect(getDashboardSummary().disabledUsers).toBe(before.disabledUsers + 1)
    deleteUser(created.id)
    expect(getDashboardSummary().totalUsers).toBe(before.totalUsers)
  })

  it("protects the current administrator", () => {
    expect(() => setUserStatus(1, 0)).toThrow("当前登录账号")
    expect(() => deleteUser(1)).toThrow("当前登录账号")
  })
})
