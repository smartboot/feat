import type { RestResult } from "@/api/types"
import { clearToken, getToken } from "@/auth/token"

export class ApiError extends Error {
  readonly status: number
  readonly code?: number

  constructor(message: string, status: number, code?: number) {
    super(message)
    this.name = "ApiError"
    this.status = status
    this.code = code
  }
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set("Accept", "application/json")
  if (init.body) headers.set("Content-Type", "application/json")
  const token = getToken()
  if (token) headers.set("Authorization", `Bearer ${token}`)

  const response = await fetch(path, { ...init, headers })
  const body = await response.json().catch(() => null) as RestResult<T> | null
  if (response.status === 401) clearToken()
  if (!response.ok || !body?.success) {
    throw new ApiError(body?.message || "请求失败，请稍后重试", response.status, body?.code)
  }
  return body.data
}
