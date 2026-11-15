import { cleanup, render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, describe, expect, it, vi } from "vitest"

import type { PageAdminItem } from "@/api/types"
import { PageStudio } from "@/pages/PageStudio"

afterEach(cleanup)

const datasources = [
  {
    key: "role",
    label: "角色",
    writable: true,
    fields: [
      { key: "id", label: "ID", type: "number", writable: false },
      { key: "code", label: "角色编码", type: "string", writable: true },
      { key: "name", label: "角色名称", type: "string", writable: true },
      { key: "status", label: "状态", type: "string", writable: true },
    ],
  },
]

const listJson = {
  rowKey: "id",
  pageSize: 20,
  actions: ["create", "edit", "delete", "refresh"],
  columns: [
    { key: "name", label: "角色名称", format: "text", width: "w-36" },
    {
      key: "status",
      label: "状态",
      format: "badge",
      width: "w-24",
      options: [
        { value: "1", label: "启用" },
        { value: "0", label: "停用" },
      ],
    },
  ],
  filters: [{ key: "name", label: "名称", widget: "input", placeholder: "搜索", options: [] }],
}

const formJson = {
  createTitle: "新建角色",
  editTitle: "编辑角色",
  createDescription: "",
  editDescription: "",
  fields: [{ key: "name", label: "角色名称", widget: "input", span: 1 }],
}

const record: PageAdminItem = {
  id: 1,
  pageKey: "roles",
  title: "角色权限",
  description: "配置驱动的角色列表",
  datasourceKey: "role",
  listJson: JSON.stringify(listJson),
  formJson: JSON.stringify(formJson),
  editTime: "2026-11-15 10:00:00",
}

function renderStudio(overrides: Partial<PageAdminItem> = {}, referencing: Array<{ name: string; path: string }> = []) {
  return render(
    <PageStudio
      mode="edit"
      initial={{ ...record, ...overrides }}
      datasources={datasources}
      referencingMenus={referencing}
      pending={false}
      onSubmit={vi.fn()}
      onClose={vi.fn()}
    />,
  )
}

describe("PageStudio", () => {
  it("编辑态能渲染出分区、字段使用情况与实时预览", () => {
    renderStudio()

    // 标题同时出现在页头与预览卡片里
    expect(screen.getAllByText("角色权限").length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText("/p/roles")).toBeTruthy()
    expect(screen.getByRole("button", { name: /基本信息/ })).toBeTruthy()
    expect(screen.getByRole("button", { name: /列表视图/ })).toBeTruthy()
    // 字段面板把「这个字段被谁用了」摊开
    expect(screen.getByText("2 列 · 1 筛选")).toBeTruthy()
    expect(screen.getByText("列 1")).toBeTruthy()
    // 预览用真实单元格渲染，badge 映射生效
    expect(screen.getAllByText("启用").length).toBeGreaterThan(0)
  })

  it("发布检查会列出不能保存的问题与影响面", async () => {
    renderStudio({ listJson: JSON.stringify({ ...listJson, columns: [] }) })
    // 页头的问题计数可直接跳到发布检查
    await userEvent.click(screen.getByRole("button", { name: /个问题/ }))
    expect(screen.getByText(/列表至少要有一列/)).toBeTruthy()
    expect(screen.getByText(/还没有菜单指向/)).toBeTruthy()
  })

  it("尚未保存的编辑会在页头标出来", async () => {
    renderStudio()
    expect(screen.queryByText(/有未保存的修改/)).toBeNull()
    await userEvent.click(screen.getByRole("button", { name: /基本信息/ }))
    await userEvent.type(screen.getByLabelText("页面标题 *"), "（改）")
    expect(screen.getByText("/p/roles · 有未保存的修改")).toBeTruthy()
  })
})

/**
 * 联动：左侧分区 / 悬停 / 点选，与右侧预览是同一份状态。
 * 这几条用例盯的是「改的时候看得见改的那一处」，不是像素。
 */
describe("PageStudio 与预览的联动", () => {
  it("切到表单分区时，预览自动跟到新建弹窗", async () => {
    renderStudio()
    expect(document.querySelector('[data-preview-mode="list"]')).toBeTruthy()

    await userEvent.click(screen.getByRole("button", { name: "表单 1 字段" }))
    expect(document.querySelector('[data-preview-mode="create"]')).toBeTruthy()
  })

  it("手动选过预览模式后，切分区不再抢走它；点「模式已锁定」才恢复跟随", async () => {
    renderStudio()
    await userEvent.click(screen.getByRole("button", { name: "空态" }))
    expect(document.querySelector('[data-preview-mode="empty"]')).toBeTruthy()

    await userEvent.click(screen.getByRole("button", { name: /^列表视图/ }))
    expect(document.querySelector('[data-preview-mode="empty"]')).toBeTruthy()

    await userEvent.click(screen.getByRole("button", { name: "模式已锁定" }))
    expect(document.querySelector('[data-preview-mode="list"]')).toBeTruthy()
  })

  it("悬停编辑区的一列，预览里对应的表头与单元格跟着亮；移开就灭", async () => {
    renderStudio()
    await userEvent.click(screen.getByRole("button", { name: /^列表视图/ }))
    expect(document.querySelector('[data-preview-focus="column-1"]')).toBeNull()

    const row = document.getElementById("column-1") as HTMLElement
    await userEvent.hover(row)
    expect(document.querySelector('[data-preview-focus="column-1"]')).toBeTruthy()
    expect(screen.getByText("已定位 第 2 列「状态」")).toBeTruthy()

    await userEvent.unhover(row)
    expect(document.querySelector('[data-preview-focus="column-1"]')).toBeNull()
  })

  it("点预览里的表头，编辑区跳回列表分区并选中那一列", async () => {
    renderStudio()
    // 起始分区是「字段」，点之前编辑区没有列卡片
    expect(document.getElementById("column-1")).toBeNull()

    await userEvent.click(screen.getByTitle("跳到「状态」的配置"))

    expect(document.getElementById("column-1")?.getAttribute("data-link-focus")).toBe("column-1")
    expect(screen.getByText("已定位 第 2 列「状态」")).toBeTruthy()
  })

  it("发布检查里点一条列问题，预览里同一列同时亮起来", async () => {
    renderStudio({
      listJson: JSON.stringify({ ...listJson, columns: [{ key: "name", label: "", format: "text", width: "" }] }),
    })
    await userEvent.click(screen.getByRole("button", { name: /个问题/ }))
    await userEvent.click(screen.getByText("第 1 列缺少列名"))

    // 「问题在哪」与「改的是哪」是同一个位置：编辑区高亮 + 预览同一格亮
    expect(document.getElementById("column-0")?.getAttribute("data-link-focus")).toBe("column-0")
    expect(document.querySelector('[data-preview-focus="column-0"]')).toBeTruthy()
  })

  it("字段面板里点「列 2」，落到列表分区的第 2 列并点亮预览", async () => {
    renderStudio()
    // 默认停在「字段」分区，这里只有使用情况标签，没有列卡片
    expect(document.getElementById("column-1")).toBeNull()

    await userEvent.click(screen.getByRole("button", { name: "列 2" }))

    expect(document.getElementById("column-1")?.getAttribute("data-link-focus")).toBe("column-1")
    expect(document.querySelector('[data-preview-focus="column-1"]')).toBeTruthy()
  })

  it("悬停表单里的一个字段，预览弹窗里同一字段被圈出来", async () => {
    renderStudio()
    await userEvent.click(screen.getByRole("button", { name: "表单 1 字段" }))
    expect(document.querySelector('[data-preview-focus="field-0"]')).toBeNull()

    await userEvent.hover(document.getElementById("field-0") as HTMLElement)

    expect(document.querySelector('[data-preview-focus="field-0"]')).toBeTruthy()
    expect(screen.getByText("已定位 表单字段「角色名称」")).toBeTruthy()
  })
})

/**
 * 行操作的可验证性。
 *
 * 之前行操作菜单整块禁用，配了 edit / delete 的页面在预览里只能看到一列「⋯」，
 * 「点了会怎样」全靠想象——这等于没预览。现在菜单可开，编辑切态、删除弹框，
 * 只在「确认删除」这一步停住。
 */
describe("预览里的行操作", () => {
  it("点某一行的「编辑」，切到编辑弹窗并回填那一行的示例值", async () => {
    renderStudio()
    expect(document.querySelector('[data-preview-mode="list"]')).toBeTruthy()

    await userEvent.click(screen.getAllByRole("button", { name: "行操作" })[1])
    await userEvent.click(await screen.findByText(/回填第 2 行/))

    expect(document.querySelector('[data-preview-mode="edit"]')).toBeTruthy()
    expect(screen.getByText(/示例数据第 2 行/)).toBeTruthy()
  })

  it("点「删除」弹出与线上同结构的确认框，但确认键不可用", async () => {
    renderStudio()
    await userEvent.click(screen.getAllByRole("button", { name: "行操作" })[0])
    await userEvent.click(await screen.findByText("删除"))

    expect(await screen.findByText("确认删除？")).toBeTruthy()
    expect(screen.getByText(/「角色名称1」将被删除/)).toBeTruthy()
    expect(screen.getByRole("button", { name: "确认删除" })).toBeDisabled()
  })

  it("只读账号的预览里没有行操作入口", async () => {
    renderStudio()
    await userEvent.click(screen.getByRole("button", { name: "无写权限" }))
    expect(screen.queryByRole("button", { name: "行操作" })).toBeNull()
  })
})
