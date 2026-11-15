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
const useRemoteOptions = vi.fn()

vi.mock("@/services/queries", () => ({
  useDwData: (...args: unknown[]) => useDwData(...args),
  useDwMutations: () => ({
    save: { isPending: false, mutateAsync: vi.fn() },
    remove: { isPending: false, mutateAsync: vi.fn() },
  }),
  useRemoteOptions: (...args: unknown[]) => useRemoteOptions(...args),
}))

afterEach(cleanup)

beforeEach(() => {
  useDwData.mockReset()
  useRemoteOptions.mockReset()
  useDwData.mockReturnValue({
    data: { items: [{ id: 1, title: "创建了用户", kind: "create" }], total: 1 },
    isLoading: false,
    isFetching: false,
    isError: false,
    refetch: vi.fn(),
  })
  useRemoteOptions.mockReturnValue({ data: [], isLoading: false })
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

  it("配置了 optionsApi 的筛选用远程选项，静态筛选用配置里的选项", () => {
    render(<DwList pageKey="activities" config={config} />)

    // 远程：路径与取值/显示字段原样传下去，前端不猜字段名
    const remoteCall = useRemoteOptions.mock.calls.find(
      ([path]) => path === "/api/activities/operators",
    )
    expect(remoteCall).toBeTruthy()
    expect(remoteCall?.slice(1)).toEqual(["id", "name"])

    // 静态：不发起任何远程请求，空路径被跳过
    expect(useRemoteOptions).toHaveBeenCalledWith("", "value", "label")
  })

  it("只读页面不渲染新建入口与行操作菜单", () => {
    render(<DwList pageKey="activities" config={config} />)
    expect(screen.queryByRole("button", { name: /新建/ })).toBeNull()
    expect(screen.queryByRole("button", { name: "行操作" })).toBeNull()
  })
})
