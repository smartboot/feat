import { describe, expect, it } from "vitest"

import type { DatasourceOption } from "@/api/types"
import { lintDraft, splitIssues } from "@/components/dw/lint"
import { emptyDraft, type PageDraft } from "@/components/dw/meta"

const datasource: DatasourceOption = {
  key: "role",
  label: "角色",
  writable: true,
  fields: [
    { key: "id", label: "ID", type: "number", writable: false },
    { key: "code", label: "角色编码", type: "string", writable: true },
    { key: "name", label: "角色名称", type: "string", writable: true },
    { key: "status", label: "状态", type: "string", writable: true },
  ],
}

const readOnlyDatasource: DatasourceOption = {
  key: "log",
  label: "日志",
  writable: false,
  fields: [{ key: "id", label: "ID", type: "number", writable: false }],
}

function draft(): PageDraft {
  const base = emptyDraft()
  return {
    ...base,
    pageKey: "roles",
    title: "角色权限",
    datasourceKey: "role",
    list: {
      ...base.list,
      columns: [{ key: "name", label: "角色名称", format: "text", width: "" }],
      actions: ["create", "edit", "delete", "refresh"],
    },
    form: { ...base.form, fields: [{ key: "name", label: "角色名称", widget: "input", span: 1 }] },
  }
}

describe("lintDraft", () => {
  it("一份能渲染的配置不应该有问题", () => {
    expect(lintDraft({ draft: draft(), datasources: [datasource] })).toEqual([])
  })

  it("一次给出全部问题，而不是只返回第一条", () => {
    const broken = draft()
    broken.pageKey = ""
    broken.title = ""
    // 空行主键 + 空列名 + 重复列 + 未声明字段，叠着来
    broken.list.rowKey = ""
    broken.list.columns = [
      { key: "", label: "", format: "text", width: "" },
      { key: "name", label: "", format: "badge", width: "" },
      { key: "name", label: "重复列", format: "text", width: "" },
    ]
    const issues = lintDraft({ draft: broken, datasources: [datasource] })
    const messages = issues.map((issue) => issue.message)
    expect(messages).toContain("请填写页面标识")
    expect(messages).toContain("请填写页面标题")
    expect(messages.some((message) => message.includes("行主键"))).toBe(true)
    expect(messages.some((message) => message.includes("重复出现"))).toBe(true)
    expect(messages.some((message) => message.includes("状态标签"))).toBe(true)
  })

  it("问题带定位锚点，点一下能跳到出错的列", () => {
    const broken = draft()
    broken.list.columns = [{ key: "", label: "", format: "text", width: "" }]
    const issues = lintDraft({ draft: broken, datasources: [datasource] })
    expect(issues[0].anchor).toBe("column-0")
    expect(issues[0].section).toBe("list")
  })

  it("只读数据源配写动作是错误，不是提示", () => {
    const broken = emptyDraft()
    broken.pageKey = "logs"
    broken.title = "日志"
    broken.datasourceKey = "log"
    broken.list = {
      ...broken.list,
      rowKey: "id",
      columns: [{ key: "id", label: "ID", format: "number", width: "" }],
      actions: ["delete"],
    }
    const { errors } = splitIssues(lintDraft({ draft: broken, datasources: [readOnlyDatasource] }))
    expect(errors.some((issue) => issue.message.includes("只读"))).toBe(true)
  })

  it("数据源下线后不再逐条报字段漂移，只报数据源本身", () => {
    const broken = draft()
    broken.datasourceKey = "gone"
    const issues = lintDraft({ draft: broken, datasources: [datasource] })
    expect(issues).toHaveLength(1)
    expect(issues[0].anchor).toBe("page-datasource")
  })

  it("开了新建却没有表单字段，是错误；反过来只是提示", () => {
    const noFields = draft()
    noFields.form = { ...noFields.form, fields: [] }
    const first = splitIssues(lintDraft({ draft: noFields, datasources: [datasource] }))
    expect(first.errors.some((issue) => issue.message.includes("弹窗会是空的"))).toBe(true)

    const noActions = draft()
    noActions.list = { ...noActions.list, actions: ["refresh"] }
    const second = splitIssues(lintDraft({ draft: noActions, datasources: [datasource] }))
    expect(second.errors).toHaveLength(0)
    expect(second.warnings.some((issue) => issue.message.includes("永远用不上"))).toBe(true)
  })

  it("写入策略与字段形态冲突时给出提示", () => {
    // 只读展示控件配「可编辑」不会生效
    const span = draft()
    span.form = { ...span.form, fields: [{ key: "code", label: "编码", widget: "span", writeMode: "always" }] }
    const spanIssues = splitIssues(lintDraft({ draft: span, datasources: [datasource] }))
    expect(spanIssues.warnings.some((issue) => issue.message.includes("写入策略"))).toBe(true)

    // 只读字段又要求必填，新建时永远填不了
    const readonlyRequired = draft()
    readonlyRequired.form = {
      ...readonlyRequired.form,
      fields: [{ key: "code", label: "编码", widget: "input", required: true, writeMode: "readonly" }],
    }
    const readIssues = splitIssues(lintDraft({ draft: readonlyRequired, datasources: [datasource] }))
    expect(readIssues.errors.some((issue) => issue.message.includes("必填永远无法满足"))).toBe(true)

    // 仅新增可编辑，却没有新建入口
    const noCreate = draft()
    noCreate.list = { ...noCreate.list, actions: ["edit", "refresh"] }
    noCreate.form = {
      ...noCreate.form,
      fields: [{ key: "code", label: "编码", widget: "input", writeMode: "createOnly" }],
    }
    const createIssues = splitIssues(lintDraft({ draft: noCreate, datasources: [datasource] }))
    expect(createIssues.warnings.some((issue) => issue.message.includes("新增时可填"))).toBe(true)
  })

  it("旧配置的 immutable 按「仅新增可编辑」读，不报冲突", () => {
    const legacy = draft()
    legacy.form = {
      ...legacy.form,
      fields: [{ key: "code", label: "编码", widget: "input", immutable: true, span: 1 }],
    }
    const { errors } = splitIssues(lintDraft({ draft: legacy, datasources: [datasource] }))
    expect(errors).toHaveLength(0)
  })
})
