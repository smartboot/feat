import { useState } from "react"
import { cleanup, render, screen, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, describe, expect, it, vi } from "vitest"

import type { DwFieldMeta, DwListConfig } from "@/api/types"
import { DwListConfigEditor } from "@/components/dw/DwListConfigEditor"

afterEach(cleanup)

const FIELDS: DwFieldMeta[] = [
  { key: "id", label: "ID", type: "number", writable: false },
  { key: "name", label: "角色名称", type: "string", writable: true },
  { key: "status", label: "状态", type: "string", writable: true },
  { key: "memberCount", label: "成员数", type: "number", writable: true },
]

function base(overrides: Partial<DwListConfig> = {}): DwListConfig {
  return {
    rowKey: "id",
    pageSize: 20,
    actions: ["create", "edit", "delete", "refresh"],
    columns: [
      { key: "name", label: "角色名称", format: "text", width: "w-36" },
      { key: "status", label: "状态", format: "badge", width: "w-24", options: [] },
    ],
    filters: [],
    rowActions: [],
    ...overrides,
  }
}

/**
 * 受控组件的壳：每次 onChange 都写回 state，再吐给断言。
 * 直接断言 onChange 的参数看不出「第二次编辑是否基于第一次的结果」，这里保持一致。
 */
function renderEditor(initial: DwListConfig, spy?: (list: DwListConfig) => void) {
  function Harness() {
    const [value, setValue] = useState(initial)
    return (
      <DwListConfigEditor
        value={value}
        fields={FIELDS}
        writable
        onChange={(next) => {
          setValue(next)
          spy?.(next)
        }}
      />
    )
  }
  render(<Harness />)
}

describe("列表配置：表格式行内编辑", () => {
  it("改列名直接写进配置，不需要先展开", async () => {
    const spy = vi.fn()
    renderEditor(base(), spy)

    const input = screen.getByLabelText("第 1 列的列名") as HTMLInputElement
    await userEvent.clear(input)
    await userEvent.type(input, "角色")

    expect(spy.mock.lastCall?.[0].columns[0].label).toBe("角色")
  })

  it("同一屏能看到多列的字段与格式，不用逐张卡片展开", () => {
    renderEditor(base())
    // 两列的四个格子同时在 DOM 里：列名 / 字段 / 格式 / 宽度
    expect(screen.getByLabelText("第 1 列的列名")).toBeTruthy()
    expect(screen.getByLabelText("第 2 列的列名")).toBeTruthy()
    expect(screen.getByLabelText("第 1 列的格式")).toBeTruthy()
    expect(screen.getByLabelText("第 2 列的宽度：中")).toBeTruthy()
  })

  it("宽度用刻度而不是下拉：点第 3 根条写进 w-36，再点第 1 根回到自适应", async () => {
    const spy = vi.fn()
    renderEditor(base({ columns: [{ key: "name", label: "角色名称", format: "text", width: "" }] }), spy)

    await userEvent.click(screen.getByRole("button", { name: "第 1 列的宽度：中" }))
    expect(spy.mock.lastCall?.[0].columns[0].width).toBe("w-36")

    await userEvent.click(screen.getByRole("button", { name: "第 1 列的宽度：窄" }))
    expect(spy.mock.lastCall?.[0].columns[0].width).toBe("w-24")
  })

  it("下移交换两条列的顺序", async () => {
    const spy = vi.fn()
    renderEditor(base(), spy)

    await userEvent.click(screen.getAllByRole("button", { name: "下移" })[0])

    const columns = spy.mock.lastCall?.[0].columns as Array<{ key: string }>
    expect(columns.map((column) => column.key)).toEqual(["status", "name"])
  })
})

describe("列表配置：二级配置按需展开", () => {
  it("徽标列的映射默认不占地方，点开才出现", async () => {
    renderEditor(base())
    expect(screen.queryByText(/值 → 文案映射/)).toBeNull()

    await userEvent.click(screen.getAllByRole("button", { name: "展开配置" })[1])

    expect(screen.getByText(/值 → 文案映射/)).toBeTruthy()
    // 同一时刻只展开一条：第 1 列没被带着一起展开
    expect(screen.getAllByRole("button", { name: "展开配置" })).toHaveLength(1)
  })

  it("删除放在展开区里，删掉一条不动其他列", async () => {
    const spy = vi.fn()
    renderEditor(base(), spy)

    await userEvent.click(screen.getAllByRole("button", { name: "展开配置" })[0])
    await userEvent.click(screen.getByRole("button", { name: "删除「角色名称」" }))

    expect(spy.mock.lastCall?.[0].columns.map((column: { key: string }) => column.key)).toEqual(["status"])
  })

  it("文本列展开的是一句格式说明，不是空的映射编辑器", async () => {
    renderEditor(base())
    await userEvent.click(screen.getAllByRole("button", { name: "展开配置" })[0])

    expect(screen.queryByText(/值 → 文案映射/)).toBeNull()
    expect(screen.getByText(/删掉不影响数据/)).toBeTruthy()
  })
})

describe("列表配置：注意力分层", () => {
  it("技术参数默认折叠：一屏里先看到列、筛选、动作", () => {
    renderEditor(base())
    expect(screen.queryByLabelText("每页条数")).toBeNull()
    expect(screen.getByRole("button", { name: /技术参数/ })).toBeTruthy()
  })

  it("每页条数越界时自动摊开——错误不能藏在折叠抽屉里", () => {
    renderEditor(base({ pageSize: 0 }))
    expect(screen.getByLabelText("每页条数")).toBeTruthy()
  })

  it("摘要条一眼说清全貌", () => {
    renderEditor(base({ filters: [{ key: "name", label: "名称", widget: "input", options: [] }] }))
    // 摘要条与分区标题都会写「2 列」，这里只认摘要条那一条
    const summary = within(screen.getByTestId("list-summary"))
    expect(summary.getByText("2 列")).toBeTruthy()
    expect(summary.getByText("1 筛选")).toBeTruthy()
    expect(summary.getByText("4 个动作")).toBeTruthy()
    expect(summary.getByText("每页 20 条")).toBeTruthy()
  })

  it("列空态直接给「按字段全部生成」，不用去标题右边找按钮", async () => {
    const spy = vi.fn()
    renderEditor(base({ columns: [] }), spy)

    await userEvent.click(screen.getByRole("button", { name: /按字段全部生成/ }))

    expect(spy.mock.lastCall?.[0].columns).toHaveLength(FIELDS.length)
  })

  it("没进列表的字段给出口，一点全加上", async () => {
    const spy = vi.fn()
    renderEditor(base({ columns: [{ key: "name", label: "角色名称", format: "text", width: "" }] }), spy)

    expect(screen.getByText(/还有 3 个字段没进列表/)).toBeTruthy()
    await userEvent.click(screen.getByRole("button", { name: "全部加入" }))

    expect(spy.mock.lastCall?.[0].columns).toHaveLength(4)
  })
})

describe("列表配置：动作合并成一块", () => {
  it("工具栏与行内动作分两行，行主键紧贴在行内动作旁边", () => {
    renderEditor(base())
    expect(screen.getByText("工具栏")).toBeTruthy()
    expect(screen.getByText("每一行")).toBeTruthy()
    // 行主键不再是顶部一个孤立的技术字段，而是在「每一行」这一行里
    expect(document.getElementById("row-key")?.textContent).toContain("定位这一行")
    expect(screen.getByLabelText("行主键字段")).toBeTruthy()
  })

  it("自定义按钮是表格行：名称 / 接口 / 提示框三列并排", () => {
    renderEditor(
      base({
        rowActions: [
          { label: "重置密码", confirm: "旧密码会立即失效", api: "/api/users/reset-password" },
          { label: "停用", api: "/api/users/disable" },
        ],
      }),
    )

    expect(screen.getByLabelText("第 1 个自定义按钮的名称")).toHaveProperty("value", "重置密码")
    expect(screen.getByLabelText("第 1 个自定义按钮的接口")).toHaveProperty("value", "/api/users/reset-password")
    expect(screen.getByLabelText("第 2 个自定义按钮的提示框")).toHaveProperty("value", "")
  })
})
