import type { LoginResponse, PageResult, UserListItem } from "@/api/types"
import { demoUser, seedUsers, trend7 } from "@/mocks/seed"

export const users = seedUsers
export const loginTrend = trend7

export async function mockLogin(username: string, password: string): Promise<LoginResponse> {
  await wait(650)
  if (username !== "admin" || password !== "admin123") throw new Error("用户名或密码错误，请使用演示账号登录")
  return { accessToken: "feat-plus-demo-token", tokenType: "Bearer", expiresIn: 7200, user: demoUser }
}

export async function mockCurrentUser() {
  await wait(260)
  return demoUser
}

export async function mockUsers(options: { page: number; pageSize: number; keyword: string; status: string }): Promise<PageResult<UserListItem>> {
  await wait(350)
  const keyword = options.keyword.trim().toLowerCase()
  const filtered = users.filter((user) => {
    const keywordMatched = !keyword || [user.username, user.displayName, user.department, user.email].some((value) => value.toLowerCase().includes(keyword))
    const statusMatched = options.status === "all" || String(user.status) === options.status
    return keywordMatched && statusMatched
  })
  const start = (options.page - 1) * options.pageSize
  return { items: filtered.slice(start, start + options.pageSize), total: filtered.length, page: options.page, pageSize: options.pageSize }
}

function wait(milliseconds: number) { return new Promise((resolve) => window.setTimeout(resolve, milliseconds)) }
