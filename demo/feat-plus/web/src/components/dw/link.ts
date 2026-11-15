import type { DwFieldMeta } from "@/api/types"

import type { DwIssue } from "./lint"
import { columnTitle, type PageDraft } from "./meta"

/**
 * 编辑区 ↔ 预览的双向联动模型。
 *
 * 页面配置里有三类条目会同时出现在左右两边：列、筛选、表单字段。用同一套下标
 * （`column-3` / `filter-1` / `field-2`）引用它们，两边就天然对得上：
 * 左边悬停第 2 列 → 右边那列亮；右边点表头 → 左边滚到那一列。
 *
 * 下标是**短暂的界面状态**：不进配置、不进 JSON、不入库。删掉一列之后所有下标右移，
 * 所以每次渲染都按「当前下标还存不存在」做一次存在性判断，而不是缓存条目对象。
 */
export type DwFocusKind = "column" | "filter" | "field"

export interface DwFocusTarget {
  kind: DwFocusKind
  index: number
}

/** 预览元素的锚点，同时也是编辑区条目卡片的 DOM id（lint 的 anchor 用的就是同一套）。 */
export function focusAnchor(target: DwFocusTarget): string {
  return `${target.kind}-${target.index}`
}

export function focusKey(target?: DwFocusTarget | null): string {
  return target ? focusAnchor(target) : ""
}

export function sameFocus(a?: DwFocusTarget | null, b?: DwFocusTarget | null): boolean {
  if (!a || !b) return false
  return a.kind === b.kind && a.index === b.index
}

/** 条目所属的编辑分区：表单字段在「表单」，列与筛选在「列表视图」。 */
export function sectionOfFocus(target: DwFocusTarget): "list" | "form" {
  return target.kind === "field" ? "form" : "list"
}

export type DwMarkLevel = "error" | "warning"

/** 每类条目被体检判定的最严重级别，用来在预览里标出「这一格有问题」。 */
export type DwMarks = Record<DwFocusKind, Record<number, DwMarkLevel>>

export function emptyMarks(): DwMarks {
  return { column: {}, filter: {}, field: {} }
}

/**
 * 把体检结果贴回预览的具体位置。
 *
 * 只认 `column-3` / `filter-1` / `field-2` 这三种锚点；页面标识、每页条数、数据接口
 * 这类问题在预览里没有对应元素，只留在发布检查的文字列表里。
 *
 * 同一锚点上「错误优先于提示」：如果一条提示把已存在的错误覆盖成黄色，
 * 预览会把真正挡住保存的问题藏起来。
 */
export function marksFromIssues(issues: DwIssue[]): DwMarks {
  const marks = emptyMarks()
  for (const issue of issues) {
    const target = focusFromAnchor(issue.anchor)
    if (!target) continue
    if (marks[target.kind][target.index] === "error") continue
    marks[target.kind][target.index] = issue.level
  }
  return marks
}

/** 锚点 → 联动目标。发布检查里点一条问题，要靠它把预览与编辑区同时指过去。 */
export function focusFromAnchor(anchor?: string | null): DwFocusTarget | null {
  const matched = /^(column|filter|field)-(\d+)$/.exec(anchor ?? "")
  if (!matched) return null
  return { kind: matched[1] as DwFocusKind, index: Number(matched[2]) }
}

/** 状态条上的一句话：正在看的是哪一条。下标越界（刚删了这列）时返回空串。 */
export function describeFocus(draft: PageDraft, target: DwFocusTarget, fields: DwFieldMeta[]): string {
  if (target.kind === "column") {
    const column = draft.list.columns[target.index]
    if (!column) return ""
    return `第 ${target.index + 1} 列「${columnTitle(column, fields)}」`
  }
  if (target.kind === "filter") {
    const filter = draft.list.filters[target.index]
    if (!filter) return ""
    return `筛选「${filter.label || `第 ${target.index + 1} 项`}」`
  }
  const field = draft.form.fields[target.index]
  if (!field) return ""
  return `表单字段「${field.label || `第 ${target.index + 1} 项`}」`
}

/** 编辑区条目卡片认的联动参数。两个编辑器共用。 */
export interface EditorLink {
  /** 需要高亮的条目：悬停优先，其次是点击选中的 */
  focus?: DwFocusTarget | null
  marks?: DwMarks
  onHover?: (target: DwFocusTarget | null) => void
  onSelect?: (target: DwFocusTarget) => void
}
