import { cleanup, render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, describe, expect, it, vi } from "vitest"

import type { DatasourceOption, DwListConfig, DwFormConfig, DwRow } from "@/api/types"
import { DwListView } from "@/components/dw/DwListView"
import { DwPreview } from "@/components/dw/DwPreview"
import type { PageDraft } from "@/components/dw/meta"

/**
 * 同源断言：预览与线上必须是**同一套渲染**。
 *
 * 这一组测试在重构前会失败——当时预览是照抄的第二套（`DwPreview` 里内联了
 * 501 行表格），已经和线上分叉三处：列名为空时预览兜底成字段标签（把「漏配」藏起来）、
 * 空态少了「清除筛选」出口、筛选控件是另一套实现。
 *
 * 所以这里断言的不是「预览好看」，而是**两边逐项相同**。允许的差异只有两个，
 * 都在下面的用例里写明了理由：预览不发请求（远程选项退化成说明行）、
 * 预览会给编辑菜单项加一句「回填第 N 行」尾注。
 */

const fields: DatasourceOption["fields"] = [
  { key: "id", label: "ID", type: "number", writable: false },
  { key: "name", label: "角色名称", type: "string", writable: true },
  { key: "status", label: "状态", type: "string", writable: true },
]

const datasource: DatasourceOption = { key: "role", label: "角色", writable: true, fields }

const list: DwListConfig = {
  rowKey: "id",
  pageSize: 20,
  actions: ["create", "edit", "delete", "refresh"],
  columns: [
    { key: "name", label: "角色名称", format: "text", width: "" },
    { key: "status", label: "状态", format: "badge", width: "w-24", options: [] },
  ],
  filters: [
    { key: "name", label: "名称", widget: "input" },
    { key: "status", label: "状态", widget: "select", options: [{ value: "1", label: "启用" }] },
  ],
  rowActions: [{ label: "重置密码", confirm: "旧密码会失效", api: "/api/roles/reset" }],
}

const form: DwFormConfig = {
  createTitle: "新建角色",
  editTitle: "编辑角色",
  createDescription: "",
  editDescription: "",
  fields: [],
}

const onlineRows: DwRow[] = [
  { id: 1, name: "超级管理员", status: "1" },
  { id: 2, name: "审计员", status: "1" },
  { id: 3, name: "只读账号", status: "0" },
]

const draft: PageDraft = {
  pageKey: "roles",
  title: "角色权限",
  description: "",
  datasourceKey: "role",
  list,
  form,
}

afterEach(cleanup)

/** 线上侧：随便给几个 handler，让菜单项全部可点（否则 disabled 也会渲染，结论一样）。 */
function renderOnline(overrides: Partial<Parameters<typeof DwListView>[0]> = {}) {
  return render(
    <DwListView
      list={list}
      title="角色权限"
      createLabel={form.createTitle}
      canWrite
      rows={onlineRows}
      total={onlineRows.length}
      page={1}
      pageSize={list.pageSize}
      filterValues={{ name: "", status: "all" }}
      activeFilterCount={0}
      onFilterChange={vi.fn()}
      onResetFilters={vi.fn()}
      onCreate={vi.fn()}
      onRefresh={vi.fn()}
      onEdit={vi.fn()}
      onDelete={async () => {}}
      onRowAction={async () => {}}
      {...overrides}
    />,
  )
}

function heads(): string[] {
  return Array.from(document.querySelectorAll("thead th")).map((th) => (th.textContent ?? "").trim())
}

describe("预览与线上是同源渲染", () => {
  it("表头逐项相同（列名为空时两边也都空，不兜底成字段标签）", () => {
    renderOnline()
    const online = heads()
    cleanup()

    render(<DwPreview draft={draft} datasource={datasource} mode="list" />)
    expect(heads()).toEqual(online)
    // 操作列表头两边都有（sr-only 文本），否则说明有一侧漏了
    expect(online[online.length - 1]).toBe("操作")
    expect(online).toHaveLength(list.columns.length + 1)
  })

  it("悬停表头的联动目标落在同一列：预览里第 2 列亮起来", () => {
    render(<DwPreview draft={draft} datasource={datasource} mode="list" focus={{ kind: "column", index: 1 }} />)
    expect(document.querySelector('[data-preview-focus="column-1"]')).toBeTruthy()
  })

  it("空态是同一份：插图、文案、以及「清除筛选」这个出口两边都在", () => {
    // 线上：无数据 + 有一个筛选条件
    renderOnline({ rows: [], total: 0, activeFilterCount: 1 })
    const onlineText = document.querySelector(".p-12")?.textContent ?? ""
    expect(onlineText).toContain("没有符合条件的数据")
    expect(screen.getAllByRole("button", { name: "清除筛选" }).length).toBeGreaterThan(0)
    cleanup()

    render(<DwPreview draft={draft} datasource={datasource} mode="empty" />)
    const previewText = document.querySelector(".p-12")?.textContent ?? ""
    expect(previewText).toContain("没有符合条件的数据")
    // 预览不该漏掉这个出口——漏掉时配置者以为空态是死路
    expect(screen.getAllByRole("button", { name: "清除筛选" }).length).toBeGreaterThan(0)
  })

  it("行操作菜单的条目与顺序一致，预览只多一句「回填第 N 行」尾注", async () => {
    renderOnline()
    await userEvent.click(screen.getAllByRole("button", { name: "行操作" })[0])
    const online = screen.getAllByRole("menuitem").map((item) => (item.textContent ?? "").replace(/\s+/g, ""))
    cleanup()

    render(<DwPreview draft={draft} datasource={datasource} mode="list" />)
    await userEvent.click(screen.getAllByRole("button", { name: "行操作" })[0])
    const preview = screen
      .getAllByRole("menuitem")
      .map((item) => (item.textContent ?? "").replace(/\s+/g, "").replace(/回填第\d+行/, ""))

    expect(preview).toEqual(online)
    // 顺序：编辑 → 删除 → 自定义按钮
    expect(online).toEqual(["编辑", "删除", "重置密码"])
  })

  it("预览的确认键禁用、线上可用——差别只在「有没有 handler」，不是两套结构", async () => {
    render(<DwPreview draft={draft} datasource={datasource} mode="list" />)
    await userEvent.click(screen.getAllByRole("button", { name: "行操作" })[0])
    await userEvent.click(screen.getByText("重置密码"))

    // 结构、标题、正文完全来自配置
    expect(screen.getByText("旧密码会失效")).toBeTruthy()
    expect(screen.getByRole("button", { name: "确认执行" })).toBeDisabled()
    cleanup()

    renderOnline()
    await userEvent.click(screen.getAllByRole("button", { name: "行操作" })[0])
    await userEvent.click(screen.getByText("重置密码"))
    expect(screen.getByRole("button", { name: "确认执行" })).toBeEnabled()
  })

  it("唯一有意的差异：配置了 optionsApi 的筛选，线上出真实选项、预览出「从哪加载」说明", async () => {
    const withRemote: DwListConfig = {
      ...list,
      filters: [
        {
          key: "owner",
          label: "负责人",
          widget: "select",
          optionsApi: "/api/users",
          optionValueField: "id",
          optionLabelField: "name",
        },
      ],
    }

    // 线上：容器解析好的选项进来了
    const { unmount } = renderOnline({
      list: withRemote,
      optionsFor: () => [{ value: "7", label: "张三" }],
    })
    await userEvent.click(document.getElementById("dw-filter-owner") as HTMLElement)
    expect(await screen.findByRole("option", { name: "张三" })).toBeTruthy()
    unmount()

    // 预览：不发请求，改成一行说明
    render(
      <DwPreview draft={{ ...draft, list: withRemote }} datasource={datasource} mode="list" />,
    )
    expect(screen.getByText(/选项来自 \/api\/users，预览不取数/)).toBeTruthy()
  })
})

/**
 * 预览必须先合并字段值域再渲染。
 *
 * 线上拿到的配置是后端 `applyFieldMaps` 处理过的副本，预览拿到的是编辑器里的原始
 * draft——列上的 `options` 还空着。少了合并这一步，配好值域的列在预览里显示原始编码，
 * 保存后线上却显示中文。这时候预览不是「略有出入」，而是把配置者往反方向带：
 * 他会以为值域没生效，回列表里再配一份，于是同一字段有了两份会漂移的映射。
 *
 * 这条断言就是钉住这个回归的：值域只配在字段上（列表列留空），预览也必须显示文案。
 */
describe("预览继承字段值域", () => {
  it("列留空、值域配在字段上时，单元格显示文案而不是原始编码", () => {
    const inherited: DwListConfig = {
      ...list,
      columns: [{ key: "status", label: "状态", format: "badge", width: "w-24", options: [] }],
      filters: [],
      fieldMaps: {
        status: [
          { value: "1", label: "已启用", variant: "success" },
          { value: "0", label: "已停用" },
        ],
      },
    }

    render(<DwPreview draft={{ ...draft, list: inherited }} datasource={datasource} mode="list" />)

    const cells = Array.from(document.querySelectorAll("tbody td")).map((cell) => (cell.textContent ?? "").trim())
    expect(cells).toContain("已启用")
    // 原始编码不该出现在任何一格：出现了就说明合并没生效
    expect(cells).not.toContain("1")
  })
})
