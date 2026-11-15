import { describe, expect, it } from "vitest"

import type { DwFieldMeta } from "@/api/types"
import type { DwIssue } from "@/components/dw/lint"
import { describeFocus, marksFromIssues, sameFocus, sectionOfFocus } from "@/components/dw/link"
import { EMPTY_FORM, EMPTY_LIST, columnFromField, fieldFromMeta, type PageDraft } from "@/components/dw/meta"

const fields: DwFieldMeta[] = [
  { key: "id", label: "ID", type: "number", writable: false },
  { key: "name", label: "角色名称", type: "string", writable: true },
]

function draftOf(): PageDraft {
  return {
    pageKey: "roles",
    title: "角色权限",
    description: "",
    datasourceKey: "role",
    list: {
      ...EMPTY_LIST,
      columns: [columnFromField(fields[1])],
      filters: [{ key: "name", label: "", widget: "input", placeholder: "", options: [] }],
    },
    form: { ...EMPTY_FORM, fields: [fieldFromMeta(fields[1])] },
  }
}

describe("marksFromIssues", () => {
  it("把问题贴到列 / 筛选 / 表单字段上，忽略没有预览位置的问题", () => {
    const issues: DwIssue[] = [
      { section: "basic", level: "error", message: "请填写页面标识", anchor: "page-key" },
      { section: "list", level: "error", message: "第 1 列缺少列名", anchor: "column-0" },
      { section: "list", level: "warning", message: "筛选提示", anchor: "filter-0" },
      { section: "form", level: "warning", message: "表单提示", anchor: "field-0" },
    ]
    const marks = marksFromIssues(issues)
    expect(marks.column[0]).toBe("error")
    expect(marks.filter[0]).toBe("warning")
    expect(marks.field[0]).toBe("warning")
    // 页面标识这类问题在预览里没有对应元素
    expect(Object.keys(marks.column)).toHaveLength(1)
  })

  it("同一锚点上错误优先于提示，否则真正挡住保存的问题会被黄色盖掉", () => {
    const marks = marksFromIssues([
      { section: "form", level: "error", message: "缺选项", anchor: "field-1" },
      { section: "form", level: "warning", message: "一个提示", anchor: "field-1" },
    ])
    expect(marks.field[1]).toBe("error")
  })
})

describe("describeFocus", () => {
  it("用业务文案说清正在看哪一条：列名缺失时退化成选择器与序号", () => {
    expect(describeFocus(draftOf(), { kind: "column", index: 0 }, fields)).toBe("第 1 列「角色名称」")
    expect(describeFocus(draftOf(), { kind: "filter", index: 0 }, fields)).toBe("筛选「第 1 项」")
    expect(describeFocus(draftOf(), { kind: "field", index: 0 }, fields)).toBe("表单字段「角色名称」")
  })

  it("下标越界（刚删掉这一列）时返回空串，状态条不显示残留文案", () => {
    expect(describeFocus(draftOf(), { kind: "column", index: 9 }, fields)).toBe("")
  })
})

describe("focus 的匹配与归属", () => {
  it("同 kind 同 index 才算同一条", () => {
    expect(sameFocus({ kind: "column", index: 1 }, { kind: "column", index: 1 })).toBe(true)
    expect(sameFocus({ kind: "column", index: 1 }, { kind: "field", index: 1 })).toBe(false)
    expect(sameFocus(null, { kind: "column", index: 1 })).toBe(false)
  })

  it("表单字段归表单分区，列与筛选归列表分区", () => {
    expect(sectionOfFocus({ kind: "field", index: 0 })).toBe("form")
    expect(sectionOfFocus({ kind: "column", index: 0 })).toBe("list")
    expect(sectionOfFocus({ kind: "filter", index: 0 })).toBe("list")
  })
})
