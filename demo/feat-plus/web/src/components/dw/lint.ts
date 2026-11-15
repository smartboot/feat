import type { DatasourceOption } from "@/api/types"

import type { PageDraft } from "./meta"

/**
 * 页面配置的保存前体检。
 *
 * 与 `PageService` 的校验规则逐条对齐——这里不重复造规则，只做两件事：
 * ① 把「第一条错误就返回」改成「一次给出全部问题」，配置者一轮能改完；
 * ② 给每条问题一个可点击的定位锚点，点一下跳到对应控件。
 *
 * 最终闸门仍在后端。前端这层的价值是即时反馈，不是替代校验：
 * 后端拒绝了必须能看懂、能定位，而不是只弹一句红字。
 */
export type DwIssueLevel = "error" | "warning"

/** 问题所属分区。发布检查是汇总视图，不产生自己的问题。 */
export type DwIssueSection = "basic" | "list" | "form"

export interface DwIssue {
  section: DwIssueSection
  level: DwIssueLevel
  message: string
  /** 定位锚点：控件 DOM id 的前缀，用于「跳到出问题的地方」。 */
  anchor?: string
}

export interface LintContext {
  draft: PageDraft
  datasources: DatasourceOption[]
}

const KEY_PATTERN = /^[a-z0-9_-]{1,32}$/
const API_PATH_PATTERN = /^\/api\/[A-Za-z0-9/_.-]{2,120}$/

export function lintDraft({ draft, datasources }: LintContext): DwIssue[] {
  const issues: DwIssue[] = []
  const datasource = datasources.find((ds) => ds.key === draft.datasourceKey)
  const fields = datasource?.fields ?? []
  const allKeys = new Set(fields.map((field) => field.key))
  const writableKeys = new Set(fields.filter((field) => field.writable).map((field) => field.key))

  // ---------- 基本信息 ----------
  if (!draft.pageKey.trim()) {
    issues.push({ section: "basic", level: "error", message: "请填写页面标识", anchor: "page-key" })
  } else if (!KEY_PATTERN.test(draft.pageKey.trim())) {
    issues.push({
      section: "basic",
      level: "error",
      message: "页面标识只能包含小写字母、数字、下划线和短横线（1–32 位）",
      anchor: "page-key",
    })
  }
  if (!draft.title.trim()) {
    issues.push({ section: "basic", level: "error", message: "请填写页面标题", anchor: "page-title" })
  } else if (draft.title.trim().length > 64) {
    issues.push({ section: "basic", level: "error", message: "页面标题不能超过 64 个字符", anchor: "page-title" })
  }
  if (!draft.datasourceKey) {
    issues.push({ section: "basic", level: "error", message: "请选择数据源", anchor: "page-datasource" })
  } else if (!datasource) {
    issues.push({
      section: "basic",
      level: "error",
      message: `数据源「${draft.datasourceKey}」不在编译期白名单里，可能已下线，请重新选择`,
      anchor: "page-datasource",
    })
  }

  // 数据源不可用时，列 / 筛选 / 表单的字段命中检查全部没有意义，会淹没真正的问题。
  if (!datasource) {
    return issues
  }

  const { list, form } = draft

  // ---------- 列表 ----------
  if (list.pageSize < 1 || list.pageSize > 200) {
    issues.push({ section: "list", level: "error", message: "每页条数必须在 1 到 200 之间", anchor: "page-size" })
  }
  if (!allKeys.has(list.rowKey)) {
    issues.push({
      section: "list",
      level: "error",
      message: `行主键「${list.rowKey || "未选择"}」不是数据源声明过的字段`,
      anchor: "row-key",
    })
  }
  if (list.columns.length === 0) {
    issues.push({
      section: "list",
      level: "error",
      message: "列表至少要有一列，可用「按字段生成」一次建好",
      anchor: "list-columns",
    })
  }

  const seenColumns = new Set<string>()
  list.columns.forEach((column, index) => {
    const where = `第 ${index + 1} 列`
    const anchor = `column-${index}`
    if (!column.key) {
      issues.push({ section: "list", level: "error", message: `${where}还没有选择字段`, anchor })
      return
    }
    if (seenColumns.has(column.key)) {
      issues.push({
        section: "list",
        level: "error",
        message: `${where}的字段「${column.key}」重复出现（同一字段只能有一列）`,
        anchor,
      })
    }
    seenColumns.add(column.key)
    if (!column.label.trim()) {
      issues.push({ section: "list", level: "error", message: `${where}缺少列名`, anchor })
    }
    if (!allKeys.has(column.key)) {
      issues.push({
        section: "list",
        level: "error",
        message: `${where}的字段「${column.key}」不在当前数据源声明里`,
        anchor,
      })
    }
    if (column.format === "badge" && (column.options ?? []).length === 0) {
      issues.push({
        section: "list",
        level: "error",
        message: `${where}是状态标签，需要配置「值 → 文案」映射`,
        anchor,
      })
    }
    issues.push(...lintOptions(column.options ?? [], "list", where, anchor))
  })

  const seenFilters = new Set<string>()
  list.filters.forEach((filter, index) => {
    const where = `筛选 ${index + 1}`
    const anchor = `filter-${index}`
    if (!filter.key) {
      issues.push({ section: "list", level: "error", message: `${where}还没有选择字段`, anchor })
      return
    }
    if (seenFilters.has(filter.key)) {
      issues.push({ section: "list", level: "error", message: `${where}的字段「${filter.key}」重复出现`, anchor })
    }
    seenFilters.add(filter.key)
    if (!filter.label.trim()) {
      issues.push({ section: "list", level: "error", message: `${where}缺少名称`, anchor })
    }
    if (!allKeys.has(filter.key)) {
      issues.push({
        section: "list",
        level: "error",
        message: `${where}的字段「${filter.key}」不在当前数据源声明里`,
        anchor,
      })
    }
    if (filter.widget === "select" && (filter.options ?? []).length === 0) {
      issues.push({
        section: "list",
        level: "error",
        message: `${where}是下拉筛选，必须配置选项，否则下拉里只有「全部」`,
        anchor,
      })
    }
    issues.push(...lintOptions(filter.options ?? [], "list", where, anchor))
  })

  if (!datasource.writable && list.actions.some((action) => action !== "refresh")) {
    issues.push({
      section: "list",
      level: "error",
      message: `数据源「${datasource.label}」是只读的，不能配置新建 / 编辑 / 删除动作`,
      anchor: "list-actions",
    })
  }

  for (const [what, path, anchor] of [
    ["查询接口", list.dataApi?.query, "data-api-query"],
    ["删除接口", list.dataApi?.delete, "data-api-delete"],
  ] as const) {
    const value = (path ?? "").trim()
    if (!value) continue
    if (!API_PATH_PATTERN.test(value) || value.endsWith("/")) {
      issues.push({
        section: "list",
        level: "error",
        message: `${what}必须是 /api/ 开头的站内路径，且不以 / 结尾：${value}`,
        anchor,
      })
    }
    if (what === "删除接口" && !datasource.writable) {
      issues.push({
        section: "list",
        level: "error",
        message: "只读数据源不能配置删除接口（通道覆盖不改变可写性）",
        anchor,
      })
    }
  }

  // ---------- 表单 ----------
  if (list.actions.includes("create") && form.fields.length === 0) {
    issues.push({
      section: "form",
      level: "error",
      message: "列表开启了「新建」，但表单没有任何字段，弹窗会是空的",
      anchor: "form-fields",
    })
  }
  if (form.fields.length > 0 && !list.actions.some((action) => action === "create" || action === "edit")) {
    issues.push({
      section: "form",
      level: "warning",
      message: "配了表单字段，但列表没有开启「新建」或「编辑」，这些字段永远用不上",
      anchor: "form-fields",
    })
  }

  const seenFields = new Set<string>()
  form.fields.forEach((field, index) => {
    const where = `表单字段 ${index + 1}`
    const anchor = `field-${index}`
    if (!field.key) {
      issues.push({ section: "form", level: "error", message: `${where}还没有选择字段`, anchor })
      return
    }
    if (seenFields.has(field.key)) {
      issues.push({ section: "form", level: "error", message: `${where}的字段「${field.key}」重复出现`, anchor })
    }
    seenFields.add(field.key)
    if (!field.label.trim()) {
      issues.push({ section: "form", level: "error", message: `${where}缺少名称`, anchor })
    }
    if (!writableKeys.has(field.key)) {
      issues.push({
        section: "form",
        level: "error",
        message: `${where}的字段「${field.key}」不是当前数据源的可写字段`,
        anchor,
      })
    }
    if (field.widget === "select" && (field.options ?? []).length === 0) {
      issues.push({ section: "form", level: "error", message: `${where}是下拉字段，必须配置选项`, anchor })
    }
    if (field.required && field.widget === "span") {
      issues.push({
        section: "form",
        level: "error",
        message: `${where}是只读展示字段，不能同时是必填项`,
        anchor,
      })
    }
    if ((field.pattern ?? "").trim()) {
      try {
        new RegExp(field.pattern as string)
      } catch {
        issues.push({
          section: "form",
          level: "error",
          message: `${where}的校验正则无法编译：${field.pattern}`,
          anchor,
        })
      }
    }
    issues.push(...lintOptions(field.options ?? [], "form", where, anchor))
  })

  return issues
}

function lintOptions(
  options: Array<{ value: string }>,
  section: DwIssueSection,
  where: string,
  anchor: string,
): DwIssue[] {
  const issues: DwIssue[] = []
  const seen = new Set<string>()
  options.forEach((option, index) => {
    if (!option.value.trim()) {
      issues.push({
        section,
        level: "error",
        message: `${where}的第 ${index + 1} 个选项值为空`,
        anchor,
      })
      return
    }
    if (seen.has(option.value)) {
      issues.push({
        section,
        level: "error",
        message: `${where}的选项值重复：${option.value}`,
        anchor,
      })
    }
    seen.add(option.value)
  })
  return issues
}

export function splitIssues(issues: DwIssue[]) {
  return {
    errors: issues.filter((issue) => issue.level === "error"),
    warnings: issues.filter((issue) => issue.level === "warning"),
  }
}

export function countBySection(issues: DwIssue[]) {
  return {
    basic: issues.filter((issue) => issue.section === "basic").length,
    list: issues.filter((issue) => issue.section === "list").length,
    form: issues.filter((issue) => issue.section === "form").length,
  }
}
