import { useEffect, useRef } from "react"
import { Filter, Inbox, MoreHorizontal, Pencil, Plus, RotateCcw, Trash2 } from "lucide-react"

import type { DatasourceOption, DwFormValue } from "@/api/types"
import { DwCell } from "@/components/dw/DwCell"
import { MarkDot } from "@/components/dw/DwEditorBits"
import { DwFormFields, fieldText, initialFormValue } from "@/components/dw/DwFormFields"
import { focusAnchor, type DwFocusTarget, type DwMarks } from "@/components/dw/link"
import { ACTION_LABELS, columnTitle, sampleRows, type PageDraft } from "@/components/dw/meta"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"

/**
 * 配置的实时预览。
 *
 * 这是本次改造里最重要的一块：以前的编辑器只能保证「字段都选对了」，不能保证
 * 「配完长这样」。预览用和线上完全相同的结构和单元格渲染，只把数据换成示例行——
 * 于是「列宽挤了」「状态标签全是灰的」「表单两列排不下」这些问题在保存前就能看见。
 *
 * 预览刻意不发真实请求：配置还没入库，后端拿不到；而且预览不该因为权限 / 空数据而失败。
 *
 * ## 它同时是导航面
 *
 * 光「长这样」还不够：看得出问题之后，下一个动作是「回去改那一处」。所以预览与编辑区
 * 双向联动——左边悬停哪一条，这里对应位置亮；点表头 / 筛选 / 字段，左边滚到那一条。
 * 会改数据的按钮（刷新、删除）保持禁用，只切预览状态的按钮（新建）可点。
 */
export type PreviewMode = "list" | "empty" | "create" | "edit" | "readonly"

export const PREVIEW_MODES: Array<{ value: PreviewMode; label: string; hint: string }> = [
  { value: "list", label: "列表", hint: "有数据时的样子（示例数据）" },
  { value: "empty", label: "空态", hint: "查询无结果的样子" },
  { value: "create", label: "新建", hint: "新建弹窗，字段默认值已填入" },
  { value: "edit", label: "编辑", hint: "编辑弹窗，字段取第一行的示例值" },
  { value: "readonly", label: "无写权限", hint: "只读账号看到的样子：写按钮全部不渲染" },
]

export function DwPreview({
  draft,
  datasource,
  mode,
  focus = null,
  pinned = false,
  marks,
  onHover,
  onLocate,
  onOpenForm,
}: {
  draft: PageDraft
  datasource?: DatasourceOption
  mode: PreviewMode
  /** 当前联动焦点（悬停优先，其次是点选） */
  focus?: DwFocusTarget | null
  /**
   * 焦点来自「点选」而不是悬停。只有点选才自动滚动：悬停会频繁变化，
   * 跟着滚会让预览一直抖。
   */
  pinned?: boolean
  marks?: DwMarks
  onHover?: (target: DwFocusTarget | null) => void
  onLocate?: (target: DwFocusTarget) => void
  /** 点预览里的「新建」：切到新建态（由工作台决定要不要同时跳到表单分区） */
  onOpenForm?: (mode: "create" | "edit") => void
}) {
  const fields = datasource?.fields ?? []
  const { list, form } = draft
  const containerRef = useRef<HTMLDivElement>(null)

  const anchor = focus ? focusAnchor(focus) : ""
  useEffect(() => {
    if (!pinned || !anchor) return
    const element = containerRef.current?.querySelector(`[data-preview-focus="${anchor}"]`)
    // jsdom 没有 scrollIntoView，测试环境里跳过（这条只是体验优化）
    if (element && typeof element.scrollIntoView === "function") {
      element.scrollIntoView({ block: "nearest", inline: "nearest" })
    }
  }, [pinned, anchor])

  const isFocused = (kind: DwFocusTarget["kind"], index: number) =>
    focus?.kind === kind && focus.index === index

  /** 表头 / 筛选 / 字段共用的定位与悬停行为。没有 onLocate 就退化成纯静态图。 */
  const linkProps = (target: DwFocusTarget, label: string) =>
    onLocate
      ? {
          title: `跳到「${label}」的配置`,
          onClick: () => onLocate(target),
          onMouseEnter: onHover ? () => onHover(target) : undefined,
          onMouseLeave: onHover ? () => onHover(null) : undefined,
        }
      : {}

  if (mode === "create" || mode === "edit") {
    const editing = mode === "edit"
    const rows = sampleRows(fields, list.columns, 1)
    const values: Record<string, DwFormValue> = {}
    for (const field of form.fields) {
      // 预览里的初值走和真实弹窗同一个函数：多选字段在预览里也要是多选的样子。
      values[field.key] = initialFormValue(
        field,
        editing ? (rows[0] as Record<string, unknown> | undefined) ?? null : null,
      )
    }
    return (
      <div ref={containerRef} data-preview-mode={mode}>
        <div className="rounded-xl border bg-background p-4 shadow-sm">
          <div className="grid gap-1">
            <p className="text-base font-semibold">{editing ? form.editTitle || "编辑" : form.createTitle || "新建"}</p>
            <p className="text-xs text-muted-foreground">
              {(editing ? form.editDescription : form.createDescription) || "（未配置说明文案）"}
            </p>
          </div>
          <div className="mt-4">
            <DwFormFields
              form={form}
              values={values}
              editing={editing}
              disabled
              link={{
                index: focus?.kind === "field" ? focus.index : null,
                marks: marks?.field,
                onSelect: onLocate ? (index) => onLocate({ kind: "field", index }) : undefined,
                onHover: onHover
                  ? (index) => onHover(index == null ? null : { kind: "field", index })
                  : undefined,
              }}
            />
          </div>
          <div className="mt-5 flex justify-end gap-2">
            <Button variant="outline" size="sm" disabled>
              取消
            </Button>
            <Button size="sm" disabled>
              {editing ? "保存修改" : form.createTitle || "新建"}
            </Button>
          </div>
        </div>
      </div>
    )
  }

  const canWrite = mode !== "readonly"
  // 空态就该是空的：留着 3 行示例数据还要在下面写「没有符合条件的数据」，是自相矛盾。
  const rows = mode === "empty" ? [] : sampleRows(fields, list.columns, 3)
  const total = mode === "empty" ? 0 : 24
  const rowActions = list.actions.filter((action) => action === "edit" || action === "delete")
  const showRowMenu = canWrite && rowActions.length > 0 && rows.length > 0

  return (
    <div ref={containerRef} data-preview-mode={mode} className="space-y-3">
      {list.filters.length > 0 && (
        <Card>
          <CardContent className={cn("grid gap-2 p-3", list.filters.length > 1 ? "grid-cols-2" : "grid-cols-1")}>
            {list.filters.map((filter, index) => {
              const active = isFocused("filter", index)
              const label = filter.label || `筛选 ${index + 1}`
              return (
                <div
                  key={`${filter.key}-${index}`}
                  data-preview-focus={active ? `filter-${index}` : undefined}
                  className={cn(
                    "relative grid gap-1.5 rounded-md p-1",
                    // 日期范围要两个输入框的宽度，两列布局里占满一行才看得清。
                    filter.widget === "dateRange" && "col-span-2",
                    active && "ring-2 ring-primary/30",
                  )}
                >
                  <Label className="flex items-center gap-1.5 text-xs">
                    {label}
                    {marks?.filter[index] && <MarkDot level={marks.filter[index]} />}
                  </Label>
                  {filter.widget === "select" ? (
                    <Select value="all" disabled>
                      <SelectTrigger className="h-8">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">全部</SelectItem>
                        {(filter.options ?? []).map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label || option.value}
                          </SelectItem>
                        ))}
                        {/* 预览不发真实请求：远程选项在这里只能说清「选项从哪来」，不能假装已经取到。 */}
                        {filter.optionsApi && (
                          <SelectItem value="__remote" disabled>
                            打开页面后从 {filter.optionsApi} 加载
                          </SelectItem>
                        )}
                      </SelectContent>
                    </Select>
                  ) : filter.widget === "dateRange" ? (
                    <div className="flex items-center gap-1.5">
                      <Input type="date" className="h-8 min-w-0 flex-1" disabled />
                      <span className="shrink-0 text-[10px] text-muted-foreground">至</span>
                      <Input type="date" className="h-8 min-w-0 flex-1" disabled />
                    </div>
                  ) : (
                    <Input className="h-8" disabled placeholder={filter.placeholder || filter.label || "输入关键字"} />
                  )}
                  {onLocate && (
                    <button
                      type="button"
                      aria-label={`定位到筛选「${label}」`}
                      className="absolute inset-0 rounded-md"
                      {...linkProps({ kind: "filter", index }, label)}
                    />
                  )}
                </div>
              )
            })}
          </CardContent>
        </Card>
      )}

      <Card className="overflow-hidden">
        <CardHeader className="flex-row items-center justify-between space-y-0 border-b px-4 py-3">
          <div>
            <CardTitle className="text-sm">{draft.title || "未命名页面"}</CardTitle>
            <p className="mt-0.5 text-[11px] text-muted-foreground">共 {total} 条</p>
          </div>
          <div className="flex items-center gap-1.5">
            {list.filters.length > 0 && mode === "empty" && (
              <Button variant="ghost" size="sm" className="h-7 gap-1 text-[11px]" disabled>
                <RotateCcw className="size-3" />
                清除筛选
              </Button>
            )}
            {list.filters.length > 0 && mode === "list" && (
              <Badge variant="outline" className="gap-1 text-[10px]">
                <Filter className="size-2.5" />
                {list.filters.length} 个条件
              </Badge>
            )}
            {list.actions.includes("refresh") && (
              <Button variant="outline" size="sm" className="h-7 text-[11px]" disabled>
                刷新
              </Button>
            )}
            {canWrite && list.actions.includes("create") && (
              <Button
                size="sm"
                className="h-7 text-[11px]"
                type="button"
                title="看一眼新建弹窗，并跳到表单配置"
                onClick={() => onOpenForm?.("create")}
              >
                <Plus className="size-3" />
                {form.createTitle || "新建"}
              </Button>
            )}
          </div>
        </CardHeader>

        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                {list.columns.map((column, index) => {
                  const active = isFocused("column", index)
                  const label = columnTitle(column, fields)
                  return (
                    <TableHead
                      key={`${column.key}-${index}`}
                      data-preview-focus={active ? `column-${index}` : undefined}
                      {...linkProps({ kind: "column", index }, label)}
                      className={cn(
                        "whitespace-nowrap text-xs",
                        column.width || undefined,
                        onLocate && "cursor-pointer",
                        active && "bg-primary/10 text-foreground",
                      )}
                    >
                      <span className="inline-flex items-center gap-1.5">
                        {label}
                        {marks?.column[index] && <MarkDot level={marks.column[index]} />}
                      </span>
                    </TableHead>
                  )
                })}
                {showRowMenu && (
                  <TableHead className="w-10">
                    <span className="sr-only">操作</span>
                  </TableHead>
                )}
              </TableRow>
            </TableHeader>
            <TableBody>
              {list.columns.length === 0 && (
                <TableRow>
                  <TableCell className="p-6 text-center text-xs text-muted-foreground">
                    还没有配置列，右侧预览会是一张空表头
                  </TableCell>
                </TableRow>
              )}
              {rows.map((row, rowIndex) => (
                <TableRow key={rowIndex}>
                  {list.columns.map((column, index) => (
                    <TableCell
                      key={`${column.key}-${index}`}
                      className={cn(
                        "align-top",
                        column.format === "number" && "text-right",
                        isFocused("column", index) && "bg-primary/[0.06]",
                      )}
                    >
                      <DwCell column={column} row={row} />
                    </TableCell>
                  ))}
                  {showRowMenu && (
                    <TableCell>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon" className="size-6" disabled>
                            <MoreHorizontal className="size-3.5" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          {list.actions.includes("edit") && (
                            <DropdownMenuItem disabled>
                              <Pencil /> {ACTION_LABELS.edit}
                            </DropdownMenuItem>
                          )}
                          {list.actions.includes("delete") && (
                            <DropdownMenuItem disabled>
                              <Trash2 /> {ACTION_LABELS.delete}
                            </DropdownMenuItem>
                          )}
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>

        {mode === "empty" && (
          <div className="p-8 text-center">
            <Inbox className="mx-auto size-6 text-muted-foreground/40" />
            <p className="mt-2 text-xs font-medium">没有符合条件的数据</p>
          </div>
        )}

        <div className="flex items-center justify-between border-t px-4 py-2">
          <p className="text-[11px] text-muted-foreground">
            第 1 / {Math.max(1, Math.ceil(total / Math.max(1, list.pageSize)))} 页 · 共 {total} 条
          </p>
          <div className="flex items-center gap-1.5">
            <span className="text-[11px] text-muted-foreground">每页 {list.pageSize} 条</span>
            <Button variant="outline" size="sm" className="h-6 text-[11px]" disabled>
              上一页
            </Button>
            <Button variant="outline" size="sm" className="h-6 text-[11px]" disabled>
              下一页
            </Button>
          </div>
        </div>
      </Card>
    </div>
  )
}
