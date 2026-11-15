import { cleanup, render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { DwOptionImportDialog } from "@/components/dw/DwOptionImportDialog"

/**
 * 值映射「从接口导入」。
 *
 * 这里最值得测的不是「能导入」，而是**字段名猜错时的表现**：权限点接口返回的是
 * `{ key, label }`，如果按默认的 `value` 取，会得到一份全空的映射。全空的映射不报错、
 * 不崩，只是列表里那一格继续显示 `user:write`——比直接报错难查得多。
 */

const { fetchOptionRows } = vi.hoisted(() => ({ fetchOptionRows: vi.fn() }))

vi.mock("@/services/http-admin-service", () => ({
  adminService: { fetchOptionRows: (...args: unknown[]) => fetchOptionRows(...args) },
}))

afterEach(cleanup)

beforeEach(() => {
  fetchOptionRows.mockReset()
})

const permissionRows = [
  { key: "user:read", label: "查看成员", groupKey: "user" },
  { key: "user:write", label: "管理成员", groupKey: "user" },
  { key: "role:read", label: "查看角色", groupKey: "role" },
]

function renderDialog(onImported = vi.fn()) {
  render(
    <DwOptionImportDialog open onOpenChange={vi.fn()} existing={[]} onImported={onImported} />,
  )
  return onImported
}

async function fill(path: string, valueField?: string) {
  await userEvent.type(screen.getByLabelText("接口路径"), path)
  if (valueField) {
    const input = screen.getByLabelText("取值字段")
    await userEvent.clear(input)
    await userEvent.type(input, valueField)
  }
}

describe("从接口导入映射", () => {
  it("非站内路径直接拒绝，不发请求", async () => {
    renderDialog()
    await fill("https://example.com/users")
    await userEvent.click(screen.getByRole("button", { name: "读取" }))

    expect(screen.getByText("只支持站内 /api/ 路径")).toBeTruthy()
    expect(fetchOptionRows).not.toHaveBeenCalled()
  })

  it("按填写的取值 / 显示字段映射，并报出可用项数", async () => {
    fetchOptionRows.mockResolvedValue(permissionRows)
    renderDialog()

    await fill("/api/permissions", "key")
    await userEvent.click(screen.getByRole("button", { name: "读取" }))

    expect(fetchOptionRows).toHaveBeenCalledWith("/api/permissions")
    // 3 行全部有值，5 条预览里前 3 条列出来
    expect(await screen.findByText(/读到 3 行，可用 3 项/)).toBeTruthy()
    expect(screen.getByText("user:read → 查看成员")).toBeTruthy()
    expect(screen.getByText("role:read → 查看角色")).toBeTruthy()
  })

  it("取值字段写错时说清「读到了但映射不出」，而不是显示导入 0 项", async () => {
    fetchOptionRows.mockResolvedValue(permissionRows)
    renderDialog()

    // 默认取值字段是 value，权限点接口给的是 key
    await fill("/api/permissions")
    await userEvent.click(screen.getByRole("button", { name: "读取" }))

    expect(await screen.findByText(/读到 3 行，但按「value」取值后没有非空项——检查取值字段名/)).toBeTruthy()
    // 没有可用项时不该给出导入按钮
    expect(screen.queryByRole("button", { name: /替换为/ })).toBeNull()
  })

  it("替换会把结果交给调用方，追加则保留已有项", async () => {
    fetchOptionRows.mockResolvedValue(permissionRows)
    const onImported = renderDialog()

    await fill("/api/permissions", "key")
    await userEvent.click(screen.getByRole("button", { name: "读取" }))
    await userEvent.click(await screen.findByRole("button", { name: "替换为 3 项" }))

    expect(onImported).toHaveBeenCalledWith([
      { value: "user:read", label: "查看成员", variant: "secondary" },
      { value: "user:write", label: "管理成员", variant: "secondary" },
      { value: "role:read", label: "查看角色", variant: "secondary" },
    ])
  })

  it("接口返回的不是数组时报错，不静默变成空映射", async () => {
    fetchOptionRows.mockRejectedValue(new Error("/api/permissions 返回的不是数组，无法作为选项导入"))
    renderDialog()

    await fill("/api/permissions", "key")
    await userEvent.click(screen.getByRole("button", { name: "读取" }))

    expect(await screen.findByText(/返回的不是数组/)).toBeTruthy()
  })
})
