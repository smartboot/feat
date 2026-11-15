import { act, renderHook } from "@testing-library/react"
import { afterEach, describe, expect, it, vi } from "vitest"

import type { PageDraft } from "@/components/dw/meta"
import { isStructuralChange, useDraftHistory } from "@/components/dw/useDraftHistory"

/**
 * 撤销栈的规则断言。
 *
 * 三条约束：连续打字只算一步、结构性改动立刻成为一步、撤销回到起点时 dirty 必须变回 false
 * （否则「什么都没改」的草稿还在提示「有未保存的修改」）。
 */

function draft(): PageDraft {
  return {
    pageKey: "roles",
    title: "角色",
    description: "",
    datasourceKey: "role",
    list: {
      rowKey: "id",
      pageSize: 20,
      actions: ["refresh"],
      columns: [{ key: "name", label: "名称", format: "text", width: "" }],
      filters: [],
    },
    form: { createTitle: "新建", editTitle: "编辑", createDescription: "", editDescription: "", fields: [] },
  }
}

/** 不开 fake timers：只需要控制 Date.now，别把 React 的调度也一起搅进去。 */
let clock = 1_000_000
function setClock() {
  clock += 100
}
function rewindClock() {
  clock += 1000
}

afterEach(() => {
  vi.restoreAllMocks()
})

function mount() {
  clock = 1_000_000
  vi.spyOn(Date, "now").mockImplementation(() => clock)
  return renderHook(() => useDraftHistory(draft()))
}

describe("useDraftHistory", () => {
  it("连续打字合并成一条：撤销一次就退回原样", () => {
    const { result } = mount()
    for (const title of ["角色 A", "角色 AB", "角色 ABC"]) {
      setClock()
      act(() => result.current.setDraft((current) => ({ ...current, title })))
    }
    expect(result.current.draft.title).toBe("角色 ABC")
    expect(result.current.dirty).toBe(true)

    act(() => result.current.undo())
    expect(result.current.draft.title).toBe("角色")
    expect(result.current.canUndo).toBe(false)
    // 回到起点就不再是「有未保存的修改」——这正是版本号比字符串比对更准的地方
    expect(result.current.dirty).toBe(false)
  })

  it("加一列是独立的一步：撤销一次整列消失，之前打的字还在", () => {
    const { result } = mount()
    setClock()
    act(() => result.current.setDraft((current) => ({ ...current, title: "角色 A" })))
    setClock()
    act(() =>
      result.current.setDraft((current) => ({
        ...current,
        list: {
          ...current.list,
          columns: [...current.list.columns, { key: "code", label: "编码", format: "text", width: "" }],
        },
      })),
    )
    expect(result.current.draft.list.columns).toHaveLength(2)

    act(() => result.current.undo())
    expect(result.current.draft.list.columns).toHaveLength(1)
    expect(result.current.draft.title).toBe("角色 A")
  })

  it("撤销之后能重做，重做后又能撤销", () => {
    const { result } = mount()
    setClock()
    act(() => result.current.setDraft((current) => ({ ...current, title: "改过" })))
    act(() => result.current.undo())
    expect(result.current.canRedo).toBe(true)

    act(() => result.current.redo())
    expect(result.current.draft.title).toBe("改过")
    expect(result.current.dirty).toBe(true)
  })

  it("撤销之后又改动：redo 栈作废，新改动另起一条", () => {
    const { result } = mount()
    setClock()
    act(() => result.current.setDraft((current) => ({ ...current, title: "第一次" })))
    rewindClock()
    act(() => result.current.setDraft((current) => ({ ...current, title: "第二次" })))
    act(() => result.current.undo())
    expect(result.current.draft.title).toBe("第一次")

    setClock()
    act(() => result.current.setDraft((current) => ({ ...current, title: "另一条路" })))
    expect(result.current.canRedo).toBe(false)
    act(() => result.current.undo())
    expect(result.current.draft.title).toBe("第一次")
  })

  it("超过合并窗口的两次修改是两条历史", () => {
    const { result } = mount()
    setClock()
    act(() => result.current.setDraft((current) => ({ ...current, title: "A" })))
    rewindClock()
    act(() => result.current.setDraft((current) => ({ ...current, title: "B" })))

    act(() => result.current.undo())
    expect(result.current.draft.title).toBe("A")
  })

  it("同一份草稿重复设置不产生历史（引用相同就直接忽略）", () => {
    const { result } = mount()
    const same = result.current.draft
    act(() => result.current.setDraft(same))
    expect(result.current.canUndo).toBe(false)
    expect(result.current.dirty).toBe(false)
  })
})

describe("isStructuralChange", () => {
  it("改文字不是结构变更", () => {
    const a = draft()
    expect(isStructuralChange(a, { ...a, title: "新标题" })).toBe(false)
    expect(
      isStructuralChange(a, {
        ...a,
        list: { ...a.list, columns: [{ ...a.list.columns[0], label: "改过的列名" }] },
      }),
    ).toBe(false)
  })

  it("增删列、改顺序、换数据源都是结构变更", () => {
    const a = draft()
    expect(
      isStructuralChange(a, {
        ...a,
        list: { ...a.list, columns: [...a.list.columns, { key: "x", label: "X", format: "text", width: "" }] },
      }),
    ).toBe(true)
    expect(isStructuralChange(a, { ...a, datasourceKey: "user" })).toBe(true)
  })
})
