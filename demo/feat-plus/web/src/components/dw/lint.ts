import type { DatasourceOption } from "@/api/types"

import { FORM_WIDGET_LABELS, MAX_ROW_ACTIONS, writeModeOf, type PageDraft } from "./meta"

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
    if (filter.widget === "select") {
      // 两个来源二选一：静态选项或远程接口。两边都空等于下拉里只有「全部」。
      const api = (filter.optionsApi ?? "").trim()
      if ((filter.options ?? []).length === 0 && !api) {
        issues.push({
          section: "list",
          level: "error",
          message: `${where}是下拉筛选，必须配置选项或选项接口，否则下拉里只有「全部」`,
          anchor,
        })
      }
      if (api && !API_PATH_PATTERN.test(api)) {
        issues.push({
          section: "list",
          level: "error",
          message: `${where}的选项接口必须是 /api/ 开头的站内路径：${api}`,
          anchor,
        })
      }
    }
    if (filter.widget === "dateRange") {
      // 结束键漏配的表现是「填了结束日期、结果没变」，比报错难查得多，这里当错误挡。
      const endKey = (filter.endKey ?? "").trim()
      if (!endKey) {
        issues.push({
          section: "list",
          level: "error",
          message: `${where}是日期范围筛选，必须选择结束字段`,
          anchor,
        })
      } else if (endKey === filter.key) {
        issues.push({
          section: "list",
          level: "error",
          message: `${where}的起始与结束字段不能是同一个`,
          anchor,
        })
      } else if (!allKeys.has(endKey)) {
        issues.push({
          section: "list",
          level: "error",
          message: `${where}的结束字段「${endKey}」不在当前数据源声明里`,
          anchor,
        })
      } else if (seenFilters.has(endKey)) {
        issues.push({
          section: "list",
          level: "error",
          message: `${where}的结束字段「${endKey}」已被别的筛选占用`,
          anchor,
        })
      }
      if (endKey) seenFilters.add(endKey)
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

  // 自定义行操作：与内置写动作同一条约束，另外三项是「点了必须有事发生、且能说清是什么」。
  const rowActions = list.rowActions ?? []
  if (rowActions.length > MAX_ROW_ACTIONS) {
    issues.push({
      section: "list",
      level: "error",
      message: `自定义行操作最多 ${MAX_ROW_ACTIONS} 个（当前 ${rowActions.length} 个），再多应做成工具栏批量操作`,
      anchor: "list-row-actions",
    })
  }
  if (!datasource.writable && rowActions.length > 0) {
    issues.push({
      section: "list",
      level: "error",
      message: `数据源「${datasource.label}」是只读的，不能配置自定义行操作（它属于写动作）`,
      anchor: "list-row-actions",
    })
  }
  const seenActionLabels = new Set<string>()
  rowActions.forEach((action, index) => {
    const where = `自定义行操作 ${index + 1}`
    const anchor = "list-row-actions"
    const label = action.label.trim()
    if (!label) {
      issues.push({ section: "list", level: "error", message: `${where}缺少按钮名称`, anchor })
    } else if (label.length > 12) {
      issues.push({ section: "list", level: "error", message: `${where}的名称超过 12 个字`, anchor })
    } else if (seenActionLabels.has(label)) {
      issues.push({
        section: "list",
        level: "error",
        message: `${where}的名称「${label}」重复，下拉里两项同名无法区分`,
        anchor,
      })
    }
    if (label) seenActionLabels.add(label)
    const api = action.api.trim()
    if (!api) {
      issues.push({
        section: "list",
        level: "error",
        message: `${where}「${label || "未命名"}」还没有配置接口路径，点了不会有任何反应`,
        anchor,
      })
    } else if (!API_PATH_PATTERN.test(api) || api.endsWith("/")) {
      issues.push({
        section: "list",
        level: "error",
        message: `${where}的接口必须是 /api/ 开头的站内路径，且不以 / 结尾：${api}`,
        anchor,
      })
    }
    if ((action.confirm ?? "").length > 120) {
      issues.push({ section: "list", level: "error", message: `${where}的提示框超过 120 个字`, anchor })
    }
  })

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
    if (field.widget === "select") {
      // 与筛选同一条规则：静态选项与选项接口二选一，都空等于一个点开没有内容的勾选框。
      const api = (field.optionsApi ?? "").trim()
      if ((field.options ?? []).length === 0 && !api) {
        issues.push({
          section: "form",
          level: "error",
          message: `${where}是下拉字段，必须配置选项或选项接口`,
          anchor,
        })
      }
      if (api && (field.options ?? []).length > 0) {
        issues.push({
          section: "form",
          level: "error",
          message: `${where}同时配置了静态选项与选项接口，以哪个为准不明确`,
          anchor,
        })
      }
      if (api && !API_PATH_PATTERN.test(api)) {
        issues.push({
          section: "form",
          level: "error",
          message: `${where}的选项接口必须是 /api/ 开头的站内路径：${api}`,
          anchor,
        })
      }
    }
    if (field.multiple) {
      if (field.widget !== "select") {
        issues.push({
          section: "form",
          level: "error",
          message: `${where}只有下拉控件能多选，当前控件是「${FORM_WIDGET_LABELS[field.widget]}」`,
          anchor,
        })
      }
      if ((field.pattern ?? "").trim()) {
        issues.push({
          section: "form",
          level: "error",
          message: `${where}是多选字段，正则校验作用在一个值上，语义不成立`,
          anchor,
        })
      }
    }
    if (field.required && field.widget === "span") {
      issues.push({
        section: "form",
        level: "error",
        message: `${where}是只读展示字段，不能同时是必填项`,
        anchor,
      })
    }
    // 写入策略与字段形态的冲突放在这里说，而不是等上线后有人问「为什么改不了」。
    const writeMode = writeModeOf(field)
    if (field.widget === "span" && writeMode !== "readonly") {
      issues.push({
        section: "form",
        level: "warning",
        message: `${where}是只读展示控件，写入策略「${writeMode === "createOnly" ? "仅新增可编辑" : "可编辑"}」不会生效`,
        anchor,
      })
    }
    if (writeMode === "readonly" && field.required) {
      issues.push({
        section: "form",
        level: "error",
        message: `${where}是只读字段，必填永远无法满足；要么改成可写，要么取消必填`,
        anchor,
      })
    }
    if (writeMode === "createOnly" && !list.actions.includes("create")) {
      issues.push({
        section: "form",
        level: "warning",
        message: `${where}只在新增时可填，但列表没有开启「新建」，这个字段永远不会被填写`,
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
