import { cleanup, render, screen } from "@testing-library/react"
import { afterEach, describe, expect, it } from "vitest"

import type { DwColumn } from "@/api/types"
import { DwCell, cellText } from "@/components/dw/DwCell"

afterEach(cleanup)

function column(overrides: Partial<DwColumn> = {}): DwColumn {
  return { key: "value", label: "值", format: "text", width: "", options: [], ...overrides }
}

describe("单元格取值：映射 → 格式化 → 原值", () => {
  it("文本列也能把编码翻译成中文，视觉仍是普通文本", () => {
    const subject = column({
      key: "code",
      format: "text",
      options: [{ value: "ROLE_ADMIN", label: "超管" }],
    })
    expect(cellText("ROLE_ADMIN", subject)).toBe("超管")
  })

  it("映射优先于格式化：映射过的值不再走日期排版", () => {
    const subject = column({
      key: "day",
      valueFormat: "date",
      options: [{ value: "2026-11-15T10:00:00", label: "今天" }],
    })
    expect(cellText("2026-11-15T10:00:00", subject)).toBe("今天")
    // 没命中的值才落到格式化上
    expect(cellText("2026-11-16T10:00:00", subject)).toBe("2026-11-16")
  })

  it("未命中映射且没配格式化时原样显示", () => {
    const subject = column({ options: [{ value: "A", label: "甲" }] })
    expect(cellText("B", subject)).toBe("B")
  })

  it("日期 / 日期时间 / 千分位三种格式化", () => {
    expect(cellText("2026-11-15T10:30:00", column({ valueFormat: "date" }))).toBe("2026-11-15")
    expect(cellText("2026-11-15T10:30:00", column({ valueFormat: "datetime" }))).toBe("2026-11-15 10:30")
    expect(cellText(1234567, column({ format: "number", valueFormat: "thousands" }))).toBe("1,234,567")
    expect(cellText(-1234.5, column({ format: "number", valueFormat: "thousands" }))).toBe("-1,234.5")
  })

  it("解析不出来的值原样显示，不清空也不写 Invalid Date", () => {
    expect(cellText("待定", column({ valueFormat: "date" }))).toBe("待定")
    // 8 位纯数字不是时间戳，当毫秒会得到 1970 年
    expect(cellText("20261115", column({ valueFormat: "date" }))).toBe("20261115")
    expect(cellText("abc", column({ valueFormat: "thousands" }))).toBe("abc")
  })

  it("空值不参与格式化", () => {
    expect(cellText("", column({ valueFormat: "date" }))).toBe("")
    expect(cellText(null, column({ valueFormat: "thousands" }))).toBe("")
  })
})

describe("多值列的逐值翻译", () => {
  const permissions = column({
    key: "permissions",
    format: "tags",
    options: [
      { value: "role:read", label: "角色查看", variant: "secondary" },
      { value: "role:write", label: "角色编辑", variant: "warning" },
    ],
  })

  it("每个值单独查映射，不是把整个数组拼成一串再查", () => {
    render(<DwCell column={permissions} row={{ id: 1, permissions: ["role:read", "role:write"] }} />)
    expect(screen.getByText("角色查看")).toBeTruthy()
    expect(screen.getByText("角色编辑")).toBeTruthy()
    expect(screen.queryByText("role:read")).toBeNull()
  })

  it("映射里没有的值保持原样，不被静默丢掉", () => {
    render(<DwCell column={permissions} row={{ id: 1, permissions: ["role:read", "audit:read"] }} />)
    expect(screen.getByText("角色查看")).toBeTruthy()
    expect(screen.getByText("audit:read")).toBeTruthy()
  })
})

describe("单值标签列", () => {
  const status = column({
    key: "status",
    format: "badge",
    options: [
      { value: "1", label: "启用", variant: "success" },
      { value: "0", label: "停用", variant: "destructive" },
    ],
  })

  it("命中映射时用映射的文案", () => {
    render(<DwCell column={status} row={{ id: 1, status: "1" }} />)
    expect(screen.getByText("启用")).toBeTruthy()
  })

  it("没配映射的状态值退化成原文，而不是空白标签", () => {
    render(<DwCell column={column({ key: "status", format: "badge" })} row={{ id: 1, status: "9" }} />)
    expect(screen.getByText("9")).toBeTruthy()
  })
})
