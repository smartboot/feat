import { cleanup, render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import type { PageConfig } from "@/api/types"
import { DwList } from "@/components/dw/DwList"

/**
 * 筛选控件的取数行为。
 *
 * 断言的是「点了之后向后端发了什么」，不是「界面上显示了什么」——
 * 日期范围的坑在于起止要拆成两个筛选键（from / to），远程选项的坑在于
 * 配置里的 optionsApi 有没有真的变成一次请求。两者出错时页面都照常打开，
 * 只是筛选无效或下拉为空，靠肉眼看不出来。
 */

const useDwData = vi.fn()
const useFilterOptions = vi.fn()
const runRowAction = vi.fn()

vi.mock("@/services/queries", () => ({
  useDwData: (...args: unknown[]) => useDwData(...args),
  useDwMutations: () => ({
    save: { isPending: false, mutateAsync: vi.fn() },
    remove: { isPending: false, mutateAsync: vi.fn() },
    action: { isPending: false, mutateAsync: (...args: unknown[]) => runRowAction(...args) },
  }),
  useFilterOptions: (filters: unknown[]) => useFilterOptions(filters),
}))

afterEach(cleanup)

beforeEach(() => {
  useDwData.mockReset()
  useFilterOptions.mockReset()
  runRowAction.mockReset()
  runRowAction.mockResolvedValue("已下发一次性凭证")
  useDwData.mockReturnValue({
    data: { items: [{ id: 1, title: "创建了用户", kind: "create" }], total: 1 },
    isLoading: false,
    isFetching: false,
    isError: false,
    refetch: vi.fn(),
  })
  // 默认每个筛选都「还没取到」：一次性把数量对齐，避免索引越界
  useFilterOptions.mockImplementation((filters: unknown[]) => filters.map(() => ({ data: [] })))
})

const config: PageConfig = {
  pageKey: "activities",
  title: "审计日志",
  description: "",
  datasourceKey: "activity",
  canWrite: false,
  list: {
    rowKey: "id",
    pageSize: 20,
    actions: ["refresh"],
    columns: [{ key: "title", label: "动作", format: "text" }],
    filters: [
      { key: "kind", label: "类型", widget: "select", options: [{ value: "create", label: "新建" }] },
      {
        key: "operatorId",
        label: "操作人",
        widget: "select",
        optionsApi: "/api/activities/operators",
        optionValueField: "id",
        optionLabelField: "name",
      },
      { key: "from", label: "时间范围", widget: "dateRange", endKey: "to" },
    ],
  },
  form: {
    createTitle: "新建",
    editTitle: "编辑",
    createDescription: "",
    editDescription: "",
    fields: [],
  },
}

/** 最后一次查询带的筛选条件。翻页与改筛选都会重算，所以永远看最后一次。 */
function lastFilters(): Record<string, string> {
  const calls = useDwData.mock.calls
  const query = calls[calls.length - 1][1] as { filters: Record<string, string> }
  return query.filters
}

describe("DwList 的筛选取数", () => {
  it("日期范围拆成起止两个筛选键，不拼成一个值", async () => {
    const user = userEvent.setup()
    render(<DwList pageKey="activities" config={config} />)

    const start = screen.getByLabelText("时间范围")
    const end = screen.getByLabelText("时间范围（结束）")
    // initFilters 之后未填写时不应该带上空条件
    expect(lastFilters()).toEqual({})

    await user.type(start, "2026-08-01")
    expect(lastFilters()).toEqual({ from: "2026-08-01" })

    await user.type(end, "2026-08-30")
    expect(lastFilters()).toEqual({ from: "2026-08-01", to: "2026-08-30" })
  })

  it("未配置 endKey 时结束输入框不可用，不会静默丢条件", () => {
    render(
      <DwList
        pageKey="activities"
        config={{
          ...config,
          list: {
            ...config.list,
            filters: [{ key: "from", label: "时间范围", widget: "dateRange" }],
          },
        }}
      />,
    )
    expect(screen.getByLabelText("时间范围（结束）")).toBeDisabled()
  })

  it("配置了 optionsApi 的筛选把路径与取值/显示字段交给取数层，静态筛选不交给它", () => {
    render(<DwList pageKey="activities" config={config} />)

    const [filters] = useFilterOptions.mock.calls[0] as [Array<Record<string, unknown>>]
    // 三个筛选项逐个过一遍取数层：远程的带上接口，静态的不带（enabled 由路径是否为空决定）
    expect(filters).toHaveLength(3)
    expect(filters[1]).toMatchObject({
      optionsApi: "/api/activities/operators",
      optionValueField: "id",
      optionLabelField: "name",
    })
    expect(filters[0].optionsApi).toBeUndefined()
    expect(filters[2].optionsApi).toBeUndefined()
  })

  it("远程接口返回的选项真的进了下拉", async () => {
    useFilterOptions.mockReturnValue([
      { data: [] },
      { data: [{ value: "7", label: "张三", group: "", groupLabel: "" }] },
      { data: [] },
    ])
    render(<DwList pageKey="activities" config={config} />)

    // SelectTrigger 不是表单控件，label 的 htmlFor 指不到它，只能按 id 取
    await userEvent.click(document.getElementById("dw-filter-operatorId") as HTMLElement)

    expect(await screen.findByRole("option", { name: "张三" })).toBeTruthy()
  })

  it("只读页面不渲染新建入口与行操作菜单", () => {
    render(<DwList pageKey="activities" config={config} />)
    expect(screen.queryByRole("button", { name: /新建/ })).toBeNull()
    expect(screen.queryByRole("button", { name: "行操作" })).toBeNull()
  })
})

/**
 * 自定义行操作的调用契约：**主键来自当前行，路径来自配置**。
 *
 * 这两件事错了都不会报错——点下去只是「什么也没发生」或者「操作了别人的行」，
 * 所以这里直接断言发出去的是什么，而不是断言弹出了什么。
 */
describe("DwList 的自定义行操作", () => {
  const writableConfig: PageConfig = {
    ...config,
    canWrite: true,
    list: {
      ...config.list,
      actions: ["edit", "delete", "refresh"],
      rowActions: [
        { label: "重置密码", confirm: "旧密码会立即失效，并下发一次性凭证。", api: "/api/users/reset-password" },
        { label: "重新分配", api: "/api/users/reassign" },
      ],
    },
  }

  it("没配提示框的按钮点了直接调用，请求带上配置的路径与当前行主键", async () => {
    render(<DwList pageKey="users" config={writableConfig} />)

    await userEvent.click(screen.getByRole("button", { name: "行操作" }))
    await userEvent.click(await screen.findByText("重新分配"))

    expect(runRowAction).toHaveBeenCalledWith({
      api: "/api/users/reassign",
      rowKey: "id",
      id: 1,
    })
  })

  it("配了提示框的按钮先确认，正文取配置文案，确认后才发出请求", async () => {
    render(<DwList pageKey="users" config={writableConfig} />)

    await userEvent.click(screen.getByRole("button", { name: "行操作" }))
    await userEvent.click(await screen.findByText("重置密码"))

    expect(await screen.findByText("旧密码会立即失效，并下发一次性凭证。")).toBeTruthy()
    expect(runRowAction).not.toHaveBeenCalled()

    await userEvent.click(screen.getByRole("button", { name: "确认执行" }))
    expect(runRowAction).toHaveBeenCalledWith({
      api: "/api/users/reset-password",
      rowKey: "id",
      id: 1,
    })
  })

  it("只有自定义按钮、没有编辑 / 删除时，行操作入口照样出现", async () => {
    render(
      <DwList
        pageKey="users"
        config={{ ...writableConfig, list: { ...writableConfig.list, actions: ["refresh"] } }}
      />,
    )
    expect(screen.getByRole("button", { name: "行操作" })).toBeTruthy()
  })
})
